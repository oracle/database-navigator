/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.editor.data;

import com.dbn.common.action.DataKeys;
import com.dbn.common.action.Lookups;
import com.dbn.common.dispose.DisposableUserDataHolderBase;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.transaction.TransactionAction;
import com.dbn.connection.transaction.TransactionListener;
import com.dbn.data.grid.options.DataGridSettingsChangeListener;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.filter.DatasetFilterType;
import com.dbn.editor.data.model.DatasetEditorModel;
import com.dbn.editor.data.model.DatasetEditorModelRow;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.editor.data.record.ui.DatasetRecordEditorDialog;
import com.dbn.editor.data.state.DatasetEditorState;
import com.dbn.editor.data.state.column.DatasetColumnSetup;
import com.dbn.editor.data.state.column.DatasetColumnState;
import com.dbn.editor.data.structure.DatasetEditorStructureViewModel;
import com.dbn.editor.data.ui.DatasetEditorForm;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.editor.data.DatasetEditorStatus.CONNECTED;
import static com.dbn.editor.data.DatasetEditorStatus.LOADED;
import static com.dbn.editor.data.DatasetEditorStatus.LOADING;
import static com.dbn.editor.data.filter.DatasetFilterManager.EMPTY_FILTER;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
@Getter
public class DatasetEditor extends DisposableUserDataHolderBase implements
        FileEditor,
        DatabaseContextBase,
        DataProvider,
        StatefulDisposable {

    private static final DatasetLoadInstructions COL_VISIBILITY_STATUS_CHANGE_LOAD_INSTRUCTIONS = new DatasetLoadInstructions(DatasetLoadInstruction.USE_CURRENT_FILTER, DatasetLoadInstruction.PRESERVE_CHANGES, DatasetLoadInstruction.DELIBERATE_ACTION, DatasetLoadInstruction.REBUILD);
    private static final DatasetLoadInstructions CON_STATUS_CHANGE_LOAD_INSTRUCTIONS = new DatasetLoadInstructions(DatasetLoadInstruction.USE_CURRENT_FILTER);

    private final ProjectRef project;
    private final DBObjectRef<DBDataset> dataset;
    private final DBEditableObjectVirtualFile databaseFile;
    private final DatasetEditorStatusHolder status;
    private final ConnectionRef connection;
    private final DataEditorSettings settings;
    private DatasetEditorForm editorForm;
    private StructureViewModel structureViewModel;
    private String dataLoadError;

    private DatasetEditorState editorState = new DatasetEditorState();

    public DatasetEditor(@NotNull DBEditableObjectVirtualFile databaseFile, DBDataset dataset) {
        Project project = dataset.getProject();
        this.project = ProjectRef.of(project);
        this.databaseFile = databaseFile;
        this.dataset = DBObjectRef.of(dataset);
        this.settings = DataEditorSettings.getInstance(project);

        connection = ConnectionRef.of(dataset.getConnection());
        status = new DatasetEditorStatusHolder();
        status.set(CONNECTED, true);
        editorForm = new DatasetEditorForm(this);

/*
        if (!EditorUtil.hasEditingHistory(databaseFile, project)) {
            load(true, true, false);
        }
*/
        ProjectEvents.subscribe(project, this, TransactionListener.TOPIC, transactionListener);
        ProjectEvents.subscribe(project, this, ConnectionStatusListener.TOPIC, connectionStatusListener);
        ProjectEvents.subscribe(project, this, DataGridSettingsChangeListener.TOPIC, dataGridSettingsChangeListener);
    }

    @NotNull
    public DBDataset getDataset() {
        return dataset.ensure();
    }

    @NotNull
    public DatasetEditorTable getEditorTable() {
        return getEditorForm().getEditorTable();
    }

    @NotNull
    public DatasetEditorForm getEditorForm() {
        return Failsafe.nn(editorForm);
    }

    public void showSearchHeader() {
        getEditorForm().showSearchHeader();
    }

    @NotNull
    public DatasetEditorModel getTableModel() {
        return getEditorTable().getModel();
    }

    @NotNull
    public DBEditableObjectVirtualFile getDatabaseFile() {
        return nd(databaseFile);
    }

    @Override
    @Nullable
    public SchemaId getSchemaId() {
        return getDataset().getSchemaId();
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return guarded(DISPOSED_COMPONENT, this, e -> getEditorForm().getComponent());
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return guarded(null, this, e -> e.getEditorForm().getComponent());
    }

    @Override
    @NonNls
    @NotNull
    public String getName() {
        return "Data";
    }

    @Override
    @NotNull
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return editorState.clone();
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
        if (fileEditorState instanceof DatasetEditorState) {
            editorState = (DatasetEditorState) fileEditorState;
        }
    }

    @Override
    public boolean isModified() {
        return getTableModel().isModified();
    }

    @Override
    public boolean isValid() {
        return !isDisposed();
    }

    @Override
    public void selectNotify() {

    }

    @Override
    public void deselectNotify() {

    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    @Nullable
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Override
    @Nullable
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    @Nullable
    public StructureViewBuilder getStructureViewBuilder() {
        return new TreeBasedStructureViewBuilder() {
            @NotNull
            @Override
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return createStructureViewModel();
            }

            @NotNull
            StructureViewModel createStructureViewModel() {
                // Structure does not change. so it can be cached.
                if (structureViewModel == null) {
                    structureViewModel = new DatasetEditorStructureViewModel(DatasetEditor.this);
                }
                return structureViewModel;
            }
        };
    }

    public static DatasetEditor getSelected(Project project) {
        if (project != null) {
            FileEditor[] fileEditors = FileEditorManager.getInstance(project).getSelectedEditors();
            for (FileEditor fileEditor : fileEditors) {
                if (fileEditor instanceof DatasetEditor) {
                    return (DatasetEditor) fileEditor;
                }
            }
        }
        return null;
    }

    /*******************************************************
     *                   Model operations                  *
     *******************************************************/
    public void fetchNextRecords(int records) {
        try {
            DatasetEditorModel model = getTableModel();
            model.fetchNextRecords(records, false);
            dataLoadError = null;
        } catch (SQLException e) {
            Diagnostics.conditionallyLog(e);
            dataLoadError = e.getMessage();
/*
            String message = "Error loading data for " + getDataset().getQualifiedNameWithType() + ".\nCause: " + e.getMessage();
            MessageUtil.showErrorDialog(message, e);
*/
        } finally {
            Project project = getProject();
            ProjectEvents.notify(project,
                    DatasetLoadListener.TOPIC,
                    (listener) -> listener.datasetLoaded(getDatabaseFile()));
        }
    }

    public void loadData(final DatasetLoadInstructions instructions) {
        if (status.isNot(LOADING)) {
            ConnectionAction.invoke(txt("msg.dataEditor.title.LoadingTableData"), false, this,
                    (action) -> {
                        setLoading(true);
                        Project project = getProject();
                        ProjectEvents.notify(project,
                                DatasetLoadListener.TOPIC,
                                (listener) -> listener.datasetLoading(getDatabaseFile()));

                        Background.run(() -> {
                            DatasetEditorForm editorForm = getEditorForm();
                            try {
                                editorForm.showLoadingHint();
                                editorForm.getEditorTable().cancelEditing();
                                DatasetEditorTable oldEditorTable = instructions.isRebuild() ? editorForm.beforeRebuild() : null;
                                try {
                                    DatasetEditorModel tableModel = getTableModel();
                                    tableModel.load(instructions.isUseCurrentFilter(), instructions.isPreserveChanges());
                                    DatasetEditorTable editorTable = getEditorTable();
                                    editorTable.clearSelection();
                                } finally {
                                    if (!isDisposed()) {
                                        editorForm.afterRebuild(oldEditorTable);
                                    }
                                }
                                dataLoadError = null;
                            } catch (ProcessCanceledException e) {
                                Diagnostics.conditionallyLog(e);
                            } catch (SQLException e) {
                                Diagnostics.conditionallyLog(e);
                                dataLoadError = e.getMessage();
                                handleLoadError(e, instructions);
                            } catch (Exception e) {
                                Diagnostics.conditionallyLog(e);
                                log.error("Error loading table data", e);
                            } finally {
                                status.set(LOADED, true);
                                editorForm.hideLoadingHint();
                                setLoading(false);
                                ProjectEvents.notify(project,
                                        DatasetLoadListener.TOPIC,
                                        (listener) -> listener.datasetLoaded(getDatabaseFile()));
                            }
                        });
                    });
        }

    }

    private void handleLoadError(SQLException e, DatasetLoadInstructions instr) {
        Dispatch.run(() -> {
            checkDisposed();
            focusEditor();
            ConnectionHandler connection = getConnection();
            DatabaseMessageParserInterface messageParserInterface = connection.getMessageParserInterface();
            Project project = getProject();
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);

            DBDataset dataset = getDataset();
            DatasetFilter filter = filterManager.getActiveFilter(dataset);
            String datasetName = dataset.getQualifiedNameWithType();
            if (connection.isValid()) {
                boolean timeoutException = messageParserInterface.isTimeoutException(e);
                if (filter == null || filter == EMPTY_FILTER || filter.getError() != null || e instanceof SQLRecoverableException) {
                    if (instr.isDeliberateAction()) {
                        String message = timeoutException ?
                                txt("msg.dataEditor.error.DataLoadTimeout", datasetName) :
                                txt("msg.dataEditor.error.DataLoadFailed", datasetName, e.getMessage());

                        Messages.showErrorDialog(project, message);
                    }
                } else {
                    String message = timeoutException ?
                            txt("msg.dataEditor.error.DataLoadTimeout", datasetName) :
                            txt("msg.dataEditor.error.DataLoadInvalidFilter", datasetName, filter.getName(), e.getMessage());

                    String[] options = {
                            txt("msg.shared.button.Retry"),
                            txt("msg.dataEditor.button.EditFilter"),
                            txt("msg.dataEditor.button.RemoveFilter"),
                            txt("msg.dataEditor.button.IgnoreFilter"),
                            txt("msg.shared.button.Cancel")};

                    Messages.showErrorDialog(project, txt("msg.shared.title.Error"), message, options, 0,
                            (option) -> {
                                DatasetLoadInstructions instructions = DatasetLoadInstructions.clone(instr);
                                instructions.setDeliberateAction(true);

                                if (option == 0) {
                                    loadData(instructions);
                                } else if (option == 1) {
                                    filterManager.openFiltersDialog(dataset, false, false, DatasetFilterType.NONE, null);
                                    instructions.setUseCurrentFilter(true);
                                    loadData(instructions);
                                } else if (option == 2) {
                                    filterManager.setActiveFilter(dataset, null);
                                    instructions.setUseCurrentFilter(true);
                                    loadData(instructions);
                                } else if (option == 3) {
                                    filter.setError(e.getMessage());
                                    instructions.setUseCurrentFilter(false);
                                    loadData(instructions);
                                }
                            });
                }
            } else {
                Messages.showErrorDialog(project,  txt("msg.dataEditor.error.DataLoadCannotConnect", datasetName, e.getMessage()));
            }
        });
    }


    private void focusEditor() {
        Editors.openFileEditor(getProject(), getDatabaseFile(), true);
    }

    protected void setLoading(boolean loading) {
        if (status.set(LOADING, loading)) {
            DatasetEditorTable editorTable = getEditorTable();
            editorTable.setLoading(loading);
            UserInterface.repaint(editorTable);
        }

    }

    public void deleteRecords() {
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorModel model = getTableModel();

        int[] indexes = editorTable.getSelectedRows();
        model.deleteRecords(indexes);
    }

    public void insertRecord() {
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorModel model = getTableModel();

        int[] indexes = editorTable.getSelectedRows();
        int rowIndex = indexes.length > 0 && indexes[0] < model.getRowCount() ? indexes[0] : 0;
        model.insertRecord(rowIndex);
    }

    public void duplicateRecord() {
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorModel model = getTableModel();
        int[] indexes = editorTable.getSelectedRows();
        if (indexes.length == 1) {
            model.duplicateRecord(indexes[0]);
        }
    }

    public void openRecordEditor() {
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorModel model = getTableModel();

        int index = editorTable.getSelectedRow();
        if (index == -1) index = 0;
        DatasetEditorModelRow row = model.getRowAtIndex(index);
        if (row != null) {
            editorTable.stopCellEditing();
            editorTable.selectRow(row.getIndex());
            Dialogs.show(() -> new DatasetRecordEditorDialog(getProject(), row));
        }
    }

    public void openRecordEditor(int index) {
        if (index <= -1) return;

        DatasetEditorModel model = getTableModel();
        DatasetEditorModelRow row = model.getRowAtIndex(index);
        if (row == null) return;

        Dialogs.show(() -> new DatasetRecordEditorDialog(getProject(), row));
    }

    public boolean isInserting() {
        return getTableModel().is(INSERTING);
    }

    public boolean isLoading() {
        return status.is(LOADING);
    }

    public boolean isLoaded() {
        return status.is(LOADED);
    }

    public boolean isDirty() {
        return getTableModel().isDirty();
    }

    /**
     * The dataset is readonly. This can not be changed by the flag isReadonly
     */
    public boolean isReadonlyData() {
        return getTableModel().isReadonly();
    }

    public boolean isReadonly() {
        return editorState.isReadonly() || getTableModel().isEnvironmentReadonly();
    }

    public DatasetColumnSetup getColumnSetup() {
        return editorState.getColumnSetup();
    }

    public void setEnvironmentReadonly(boolean readonly) {
        getTableModel().setEnvironmentReadonly(readonly);
    }

    public void setReadonly(boolean readonly) {
        editorState.setReadonly(readonly);
    }

    public boolean isEditable() {
        DatasetEditorModel tableModel = getTableModel();
        ConnectionHandler connection = tableModel.getConnection();
        return tableModel.isEditable() && connection.isConnected(SessionId.MAIN);
    }

    public int getRowCount() {
        return getEditorTable().getRowCount();
    }


    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Nullable
    @Override
    public DatabaseSession getSession() {
        return getConnection().getSessionBundle().getMainSession();
    }

    /*******************************************************
     *                      Listeners                      *
     *******************************************************/
    private final ConnectionStatusListener connectionStatusListener = (connectionId, sessionId) -> {
        ConnectionHandler connection = getConnection();
        if (connection.getConnectionId() == connectionId && sessionId == SessionId.MAIN) {
            boolean connected = connection.isConnected(SessionId.MAIN);
            boolean statusChanged = getStatus().set(CONNECTED, connected);
            if (!statusChanged) return;

            Dispatch.run(() -> {
                DatasetEditorTable editorTable = getEditorTable();
                if (connected) {
                    editorTable.updateBackground(false);
                    UserInterface.repaint(editorTable);
                    if (!isReadonlyData()) {
                        loadData(CON_STATUS_CHANGE_LOAD_INSTRUCTIONS);
                    }
                } else {
                    editorTable.cancelEditing();
                    editorTable.updateBackground(true);
                    UserInterface.repaint(editorTable);
                }
            });
        }
    };

    private final TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void beforeAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action) {
            if (connection != getConnection()) return;

            DatasetEditorModel model = getTableModel();
            DatasetEditorTable editorTable = getEditorTable();
            if (action == TransactionAction.COMMIT) {
                if (editorTable.isEditing()) {
                    editorTable.stopCellEditing();
                }

                if (isInserting()) {
                    try {
                        model.postInsertRecord(true, false, true);
                    } catch (SQLException e1) {
                        Diagnostics.conditionallyLog(e1);
                        Messages.showErrorDialog(getProject(), txt("msg.dataEditor.error.CannotCreateRow",getDataset().getQualifiedNameWithType()), e1);
                        model.cancelInsert(true);
                    }
                }
            }

            if (action == TransactionAction.ROLLBACK || action == TransactionAction.ROLLBACK_IDLE) {
                if (editorTable.isEditing()) {
                    editorTable.stopCellEditing();
                }
                if (isInserting()) {
                    model.cancelInsert(true);
                }
            }
        }

        @Override
        public void afterAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action, boolean succeeded) {
            if (connection != getConnection()) return;

            DatasetEditorModel model = getTableModel();
            DatasetEditorTable editorTable = getEditorTable();
            if (action == TransactionAction.COMMIT || action == TransactionAction.ROLLBACK) {
                if (succeeded && isModified()) loadData(CON_STATUS_CHANGE_LOAD_INSTRUCTIONS);
            }

            if (action == TransactionAction.DISCONNECT) {
                editorTable.stopCellEditing();
                model.revertChanges();
                UserInterface.repaint(editorTable);
            }
        }
    };

    private final DataGridSettingsChangeListener dataGridSettingsChangeListener =
            visible -> loadData(COL_VISIBILITY_STATUS_CHANGE_LOAD_INSTRUCTIONS);


    String getDataLoadError() {
        return dataLoadError;
    }


    public List<DatasetColumnState> refreshColumnStates(@Nullable List<String> columnNames) {
        DatasetColumnSetup columnSetup = editorState.getColumnSetup();
        columnSetup.init(columnNames, getDataset());
        return columnSetup.getColumnStates();
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
        return databaseFile;
    }

    /*******************************************************
     *                   Data Provider                     *
     *******************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.DATASET_EDITOR.is(dataId)) return this;
        return null;
    }

    @Nullable
    public static DatasetEditor get(DataContext dataContext) {
        DatasetEditor datasetEditor = DataKeys.DATASET_EDITOR.getData(dataContext);
        if (datasetEditor != null) return datasetEditor;

        FileEditor fileEditor = Lookups.getFileEditor(dataContext);
        if (fileEditor instanceof DatasetEditor) {
            return (DatasetEditor) fileEditor;
        }
        return null;
    }

    @Nullable
    public static DatasetEditor get(AnActionEvent e) {
        DatasetEditor datasetEditor = e.getData((DataKeys.DATASET_EDITOR));
        if (datasetEditor == null) {
            FileEditor fileEditor = Lookups.getFileEditor(e);
            if (fileEditor instanceof DatasetEditor) {
                return (DatasetEditor) fileEditor;
            }
        } else {
            return datasetEditor;
        }
        return null;
    }

    @Override
    public String toString() {
        DBEditableObjectVirtualFile databaseFile = this.databaseFile;
        if (databaseFile == null) return DatabaseFileSystem.createObjectPath(dataset);
        return databaseFile.getPath();
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
        editorForm = null;
    }
}
