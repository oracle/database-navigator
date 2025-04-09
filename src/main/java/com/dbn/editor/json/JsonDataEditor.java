/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.editor.json;

import com.dbn.common.action.DataKeys;
import com.dbn.common.action.Lookups;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.transaction.TransactionAction;
import com.dbn.connection.transaction.TransactionListener;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.editor.data.DataEditorBase;
import com.dbn.editor.data.DataLoadInstructions;
import com.dbn.editor.data.DataLoadListener;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.filter.DatasetFilterType;
import com.dbn.editor.json.model.JsonDataEditorModel;
import com.dbn.editor.json.ui.JsonDataContentEditorForm;
import com.dbn.editor.json.ui.JsonDataEditorForm;
import com.dbn.editor.json.ui.table.JsonDataEditorTable;
import com.dbn.object.DBJsonView;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.editor.DBContentType.JSON;
import static com.dbn.editor.data.DataEditorStatus.CONNECTED;
import static com.dbn.editor.data.DataEditorStatus.LOADED;
import static com.dbn.editor.data.DataEditorStatus.LOADING;
import static com.dbn.editor.data.filter.DatasetFilterManager.EMPTY_FILTER;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
@Getter
public class JsonDataEditor extends DataEditorBase<DBJsonView> {

    private JsonDataEditorForm editorForm;
    private StructureViewModel structureViewModel;

    private JsonDataEditorState editorState = new JsonDataEditorState();

    public JsonDataEditor(@NotNull DBEditableObjectVirtualFile databaseFile, DBJsonView jsonView) {
        super(databaseFile, jsonView);

        this.editorForm = new JsonDataEditorForm(this);
        updateContentEditorState();

        Project project = jsonView.getProject();
        ProjectEvents.subscribe(project, this, TransactionListener.TOPIC, transactionListener);
        ProjectEvents.subscribe(project, this, ConnectionStatusListener.TOPIC, connectionStatusListener);
    }

    @NotNull
    public DBJsonView getJsonView() {
        return getDataset();
    }

    @NotNull
    public JsonDataEditorTable getEditorTable() {
        return getEditorForm().getEditorTable();
    }

    @NotNull
    public JsonDataEditorForm getEditorForm() {
        return Failsafe.nn(editorForm);
    }

    public JsonDataContentEditorForm getContentEditorForm() {
        return getEditorForm().getContentEditorForm();
    }

    public void showSearchHeader() {
        getEditorForm().showSearchHeader();
    }

    @NotNull
    public JsonDataEditorModel getTableModel() {
        return getEditorTable().getModel();
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
        if (fileEditorState instanceof JsonDataEditorState) {
            editorState = (JsonDataEditorState) fileEditorState;
            setContentEditorVisible(editorState.isEditorVisible());
        }
    }

    @Override
    @Nullable
    public StructureViewBuilder getStructureViewBuilder() {
/*        return new TreeBasedStructureViewBuilder() {
            @NotNull
            @Override
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return createStructureViewModel();
            }

            @NotNull
            StructureViewModel createStructureViewModel() {
                // Structure does not change. so it can be cached.
                if (structureViewModel == null) {
                    structureViewModel = new DatasetEditorStructureViewModel(JsonDataEditor.this);
                }
                return structureViewModel;
            }
        };*/

        return null;
    }

    /*******************************************************
     *                   Model operations                  *
     *******************************************************/
    public void fetchNextRecords(int records) {
        try {
            JsonDataEditorModel model = getTableModel();
            model.fetchNextRecords(records, false);
            setDataLoadError(null);
        } catch (SQLException e) {
            Diagnostics.conditionallyLog(e);
            setDataLoadError(e.getMessage());
        } finally {
            Project project = getProject();
            ProjectEvents.notify(project,
                    DataLoadListener.TOPIC,
                    (listener) -> listener.dataLoaded(getDatabaseFile()));
        }
    }

    public void loadData(final DataLoadInstructions instructions) {
        if (isLoading()) return;

        ConnectionAction.invoke(txt("msg.dataEditor.title.LoadingTableData"), false, this,
                (action) -> {
                    setLoading(true);
                    Project project = getProject();
                    ProjectEvents.notify(project,
                            DataLoadListener.TOPIC,
                            (listener) -> listener.dataLoading(getDatabaseFile()));

                    Background.run(() -> {
                        JsonDataEditorForm editorForm = getEditorForm();
                        try {
                            editorForm.showLoadingHint();
                            JsonDataEditorTable oldEditorTable = instructions.isRebuild() ? editorForm.beforeRebuild() : null;
                            try {
                                JsonDataEditorModel tableModel = getTableModel();
                                tableModel.load(instructions.isUseCurrentFilter(), instructions.isPreserveChanges());
                                JsonDataEditorTable editorTable = getEditorTable();
                                editorTable.clearSelection();
                            } finally {
                                editorForm.afterRebuild(oldEditorTable);
                            }
                            setDataLoadError(null);
                        } catch (ProcessCanceledException e) {
                            Diagnostics.conditionallyLog(e);
                        } catch (SQLException e) {
                            Diagnostics.conditionallyLog(e);
                            setDataLoadError(e.getMessage());
                            handleLoadError(e, instructions);
                        } catch (Exception e) {
                            Diagnostics.conditionallyLog(e);
                            log.error("Error loading table data", e);
                        } finally {
                            status.set(LOADED, true);
                            editorForm.hideLoadingHint();
                            setLoading(false);
                            ProjectEvents.notify(project,
                                    DataLoadListener.TOPIC,
                                    (listener) -> listener.dataLoaded(getDatabaseFile()));
                        }
                    });
                });

    }

    private void handleLoadError(SQLException e, DataLoadInstructions instr) {
        Dispatch.run(getComponent(), () -> {
            checkDisposed();
            focusEditor();
            ConnectionHandler connection = getConnection();
            DatabaseMessageParserInterface messageParserInterface = connection.getMessageParserInterface();
            Project project = getProject();
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);

            DBJsonView jsonView = getJsonView();
            DatasetFilter filter = filterManager.getActiveFilter(jsonView);
            String datasetName = jsonView.getQualifiedNameWithType();
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
                            txt("msg.dataEditor.error.DataLoadInvalidFilter", datasetName, e.getMessage());

                    String[] options = {
                            txt("msg.shared.button.Retry"),
                            txt("msg.dataEditor.button.EditFilter"),
                            txt("msg.dataEditor.button.RemoveFilter"),
                            txt("msg.dataEditor.button.IgnoreFilter"),
                            txt("msg.shared.button.Cancel")};

                    Messages.showErrorDialog(project, txt("msg.shared.title.Error"), message, options, 0,
                            (option) -> {
                                DataLoadInstructions instructions = DataLoadInstructions.clone(instr);
                                instructions.setDeliberateAction(true);

                                if (option == 0) {
                                    loadData(instructions);
                                } else if (option == 1) {
                                    filterManager.openFiltersDialog(jsonView, false, false, DatasetFilterType.NONE, null);
                                    instructions.setUseCurrentFilter(true);
                                    loadData(instructions);
                                } else if (option == 2) {
                                    filterManager.setActiveFilter(jsonView, null);
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


    protected void setLoading(boolean loading) {
        if (status.set(LOADING, loading)) {
            JsonDataEditorTable editorTable = getEditorTable();
            editorTable.setLoading(loading);
            UserInterface.repaint(editorTable);
        }

    }

    public void deleteRecords() {
        JsonDataEditorTable editorTable = getEditorTable();
        JsonDataEditorModel model = getTableModel();

        int[] indexes = editorTable.getSelectedRows();
        model.deleteRecords(indexes);
    }

    public void insertRecord() {
        JsonDataEditorTable editorTable = getEditorTable();
        JsonDataEditorModel model = getTableModel();

        int[] indexes = editorTable.getSelectedRows();
        int rowIndex = indexes.length > 0 && indexes[0] < model.getRowCount() ? indexes[0] : 0;
        model.insertRecord(rowIndex);
    }

    public void duplicateRecord() {
        JsonDataEditorTable editorTable = getEditorTable();
        JsonDataEditorModel model = getTableModel();
        int[] indexes = editorTable.getSelectedRows();
        if (indexes.length == 1) {
            model.duplicateRecord(indexes[0]);
        }
    }

    public boolean isInserting() {
        return getTableModel().is(INSERTING);
    }

    public boolean isDirty() {
        return getTableModel().isDirty();
    }

    /**
     * The dataset is readonly. This can not be changed by the flag isReadonly
     */
    public boolean isReadonlyData() {
        return !getJsonView().isEditable(JSON);
    }

    public boolean isReadonly() {
        return editorState.isReadonly() || getTableModel().isReadonly();
    }

    public int getRowCount() {
        return getEditorTable().getRowCount();
    }


    /*******************************************************
     *                      Listeners                      *
     *******************************************************/
    private final ConnectionStatusListener connectionStatusListener = (connectionId, sessionId) -> {
        ConnectionHandler connection = getConnection();
        if (connection.getConnectionId() != connectionId) return;
        if (sessionId != SessionId.MAIN) return;

        boolean connected = connection.isConnected(SessionId.MAIN);
        boolean statusChanged = getStatus().set(CONNECTED, connected);
        if (!statusChanged) return;

        Dispatch.run(getComponent(), () -> {
            JsonDataEditorTable editorTable = getEditorTable();
            if (connected) {
                editorTable.updateBackground(false);
                if (!isReadonlyData()) {
                    loadData(CON_STATUS_CHANGE_LOAD_INSTRUCTIONS);
                }
            } else {
                editorTable.updateBackground(true);
            }
            updateContentEditorState();
            editorTable.revalidate();
            editorTable.repaint();
        });
    };

    public void updateContentEditorState() {
        getContentEditorForm().updateEditorState();
    }

    private final TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void beforeAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action) {
            if (connection != getConnection()) return;

            JsonDataEditorModel model = getTableModel();
            JsonDataEditorTable editorTable = getEditorTable();
            if (action == TransactionAction.COMMIT) {
                if (isInserting()) {
                    try {
                        model.postInsertRecord(true, false, true);
                    } catch (SQLException e1) {
                        Diagnostics.conditionallyLog(e1);
                        Messages.showErrorDialog(getProject(), txt("msg.dataEditor.error.CannotCreateRow", getJsonView().getQualifiedNameWithType()), e1);
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

            JsonDataEditorModel model = getTableModel();
            JsonDataEditorTable editorTable = getEditorTable();
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

    /*******************************************************
     *                   Data Provider                     *
     *******************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.JSON_DATA_EDITOR.is(dataId)) return this;
        return null;
    }

    @Nullable
    public static JsonDataEditor get(DataContext dataContext) {
        JsonDataEditor jsonDataEditor = DataKeys.JSON_DATA_EDITOR.getData(dataContext);
        if (jsonDataEditor != null) return jsonDataEditor;

        FileEditor fileEditor = Lookups.getFileEditor(dataContext);
        if (fileEditor instanceof JsonDataEditor) {
            return (JsonDataEditor) fileEditor;
        }
        return null;
    }

    @Nullable
    public static JsonDataEditor get(AnActionEvent e) {
        JsonDataEditor jsonDataEditor = e.getData((DataKeys.JSON_DATA_EDITOR));
        if (jsonDataEditor != null) return jsonDataEditor;

        FileEditor fileEditor = Lookups.getFileEditor(e);
        if (fileEditor instanceof JsonDataEditor) {
            return (JsonDataEditor) fileEditor;
        }
        return null;
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
        editorForm = null;
    }

    public void toggleContentEditorVisible() {
        setContentEditorVisible(!isContentEditorVisible());
    }

    public boolean isContentEditorVisible() {
        return getEditorForm().isContentEditorVisible();
    }

    public void setContentEditorVisible(boolean visible) {
        editorState.setEditorVisible(visible);
        getEditorForm().setContentEditorVisible(visible);
    }

    public void focusContentEditor() {
        getEditorForm().getContentEditorForm().focusEditor();
    }

    public void toggleEditingLock() {
        boolean readonly = editorState.isReadonly();
        editorState.setReadonly(!readonly);
        updateContentEditorState();
    }

    public boolean isEditingLocked() {
        return editorState.isReadonly();
    }
}
