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

package com.dbn.editor.data.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.DataProviders;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.latent.Latent;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.AutoCommitLabel;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNTableScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SessionId;
import com.dbn.data.find.DataSearchComponent;
import com.dbn.data.find.SearchableDataComponent;
import com.dbn.data.grid.options.DataGridAuditColumnSettings;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.editor.DBContentType;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.state.column.DatasetColumnState;
import com.dbn.editor.data.statusbar.DatasetEditorStatusBarWidget;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DatasetEditorForm extends DBNFormBase implements SearchableDataComponent {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JLabel loadingLabel;
    private JPanel loadingIconPanel;
    private JPanel searchPanel;
    private JPanel loadingActionPanel;
    private JPanel loadingDataPanel;
    private JPanel datasetTablePanel;
    private DBNTableScrollPane datasetTableScrollPane;

    private AutoCommitLabel autoCommitLabel;
    private JPanel toolbarPanel;
    private DatasetEditorTable datasetEditorTable;
    private final WeakRef<DatasetEditor> datasetEditor;

    private final Latent<DataSearchComponent> dataSearchComponent = Latent.basic(() -> {
        DataSearchComponent dataSearchComponent = new DataSearchComponent(DatasetEditorForm.this);
        searchPanel.add(dataSearchComponent.getComponent(), BorderLayout.CENTER);
        DataProviders.register(dataSearchComponent.getSearchField(), this);
        return dataSearchComponent;
    });


    public DatasetEditorForm(DatasetEditor datasetEditor) {
        super(datasetEditor, datasetEditor.getProject());
        this.datasetEditor = WeakRef.of(datasetEditor);

        DBDataset dataset = getDataset();
        try {
            this.toolbarPanel.setBorder(Borders.insetBorder(2));

            datasetTablePanel.setBorder(Borders.tableBorder(1, 0, 0, 0));
            datasetEditorTable = new DatasetEditorTable(this, datasetEditor);
            datasetTableScrollPane.setViewportView(datasetEditorTable);
            datasetEditorTable.initTableGutter();

            ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "DBNavigator.ActionGroup.DataEditor", "", true);

            actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
            loadingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
            hideLoadingHint();

            ActionToolbar loadingActionToolbar = Actions.createActionToolbar(actionsPanel,"", true, new CancelLoadingAction());
            loadingActionPanel.add(loadingActionToolbar.getComponent(), BorderLayout.CENTER);

            Disposer.register(this, autoCommitLabel);
        } catch (SQLException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(
                    getProject(),
                    txt("msg.dataEditor.title.FailedToOpenEditor"),
                    txt("msg.dataEditor.error.FailedToOpenEditor", dataset.getQualifiedNameWithType(), e));
        }

        if (dataset.isEditable(DBContentType.DATA)) {
            ConnectionHandler connection = getConnectionHandler();
            autoCommitLabel.init(getProject(), datasetEditor.getFile(), connection, SessionId.MAIN);
        }

        Disposer.register(datasetEditor, this);
    }

    public DatasetEditorTable beforeRebuild() throws SQLException {
        Project project = ensureProject();

        DatasetEditorTable oldEditorTable = getEditorTable();
        DatasetEditor datasetEditor = getDatasetEditor();

        datasetEditorTable = new DatasetEditorTable(this, datasetEditor);
        DatasetEditorStatusBarWidget statusBarWidget = DatasetEditorStatusBarWidget.getInstance(project);
        datasetEditorTable.getSelectionModel().addListSelectionListener(e -> statusBarWidget.update());


        DataGridSettings dataGridSettings = DataGridSettings.getInstance(project);
        DataGridAuditColumnSettings auditColumnSettings = dataGridSettings.getAuditColumnSettings();

        List<TableColumn> hiddenColumns = new ArrayList<>();
        for (DatasetColumnState columnState : datasetEditor.getColumnSetup().getColumnStates()) {

            if (!columnState.isVisible() || !auditColumnSettings.isColumnVisible(columnState.getName())) {
                String columnName = columnState.getName();
                TableColumn tableColumn = datasetEditorTable.getColumnByName(columnName);
                if (tableColumn != null) {
                    hiddenColumns.add(tableColumn);
                }
            }
        }
        for (TableColumn hiddenColumn : hiddenColumns) {
            datasetEditorTable.removeColumn(hiddenColumn);
        }
        return oldEditorTable;
    }

    public void afterRebuild(final DatasetEditorTable oldEditorTable) {
        if (oldEditorTable != null) {
            Dispatch.run(() -> {
                DatasetEditorTable datasetEditorTable = getEditorTable();
                datasetTableScrollPane.setViewportView(datasetEditorTable);
                datasetEditorTable.initTableGutter();
                datasetEditorTable.updateBackground(false);

                Disposer.dispose(oldEditorTable);
            });
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    private DBDataset getDataset() {
        return getDatasetEditor().getDataset();
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return datasetEditor.ensure();
    }

    public void showLoadingHint() {
        Dispatch.run(() -> Failsafe.nn(loadingDataPanel).setVisible(true));
    }

    public void hideLoadingHint() {
        Dispatch.run(() -> Failsafe.nn(loadingDataPanel).setVisible(false));
    }

    @NotNull
    public DatasetEditorTable getEditorTable() {
        return Failsafe.nn(datasetEditorTable);
    }

    private ConnectionHandler getConnectionHandler() {
        return getEditorTable().getDataset().getConnection();
    }

    public float getHorizontalScrollProportion() {
        datasetTableScrollPane.getHorizontalScrollBar().getModel();
        return 0;
    }

    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/
    @Override
    public void showSearchHeader() {
        DatasetEditorTable editorTable = getEditorTable();
        editorTable.cancelEditing();
        editorTable.clearSelection();

        DataSearchComponent dataSearchComponent = getSearchComponent();
        dataSearchComponent.initializeFindModel();

        JTextComponent searchField = dataSearchComponent.getSearchField();
        if (searchPanel.isVisible()) {
            searchField.selectAll();
        } else {
            searchPanel.setVisible(true);    
        }
        Dispatch.run(() -> searchField.requestFocus());
    }

    private DataSearchComponent getSearchComponent() {
        return dataSearchComponent.get();
    }

    @Override
    public void hideSearchHeader() {
        getSearchComponent().resetFindModel();
        searchPanel.setVisible(false);
        DatasetEditorTable editorTable = getEditorTable();

        UserInterface.repaintAndFocus(editorTable);
    }

    @Override
    public void cancelEditActions() {
        getEditorTable().cancelEditing();
    }

    @Override
    public String getSelectedText() {
        DatasetTableCellEditor cellEditor = getEditorTable().getCellEditor();
        if (cellEditor != null) {
            return cellEditor.getTextField().getSelectedText();
        }
        return null;
    }

    @NotNull
    @Override
    public BasicTable<?> getTable() {
        return getEditorTable();
    }

    private class CancelLoadingAction extends BasicAction {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getEditorTable().getModel().cancelDataLoad();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(txt("app.shared.action.Cancel"));
            presentation.setIcon(Icons.DATA_EDITOR_STOP_LOADING);
            presentation.setEnabled(!getEditorTable().getModel().isLoadCancelled());
        }
    }


    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.DATASET_EDITOR.is(dataId)) return getDatasetEditor();
        return null;
    }
}
