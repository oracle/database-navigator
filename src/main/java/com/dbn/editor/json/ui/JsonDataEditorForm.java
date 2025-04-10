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

package com.dbn.editor.json.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
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
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.editor.DBContentType;
import com.dbn.editor.json.JsonDataEditor;
import com.dbn.editor.json.model.JsonDataEditorModelCell;
import com.dbn.editor.json.ui.table.JsonDataEditorTable;
import com.dbn.object.DBJsonView;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.DefaultFocusTraversalPolicy;
import java.sql.SQLException;

import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Splitters.setSplitPaneProportion;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class JsonDataEditorForm extends DBNFormBase implements SearchableDataComponent {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JLabel loadingLabel;
    private JPanel loadingIconPanel;
    private JPanel searchPanel;
    private JPanel loadingActionPanel;
    private JPanel loadingDataPanel;
    private JPanel tablePanel;
    private DBNTableScrollPane jsonDataTableScrollPane;

    private AutoCommitLabel autoCommitLabel;
    private JPanel toolbarPanel;
    private JPanel editorPanel;
    private JSplitPane editorSplitPanel;
    private JsonDataEditorTable jsonDataEditorTable;
    private final WeakRef<JsonDataEditor> jsonDataEditor;
    private final WeakRef<JsonDataContentEditorForm> contentEditorForm;

    public JsonDataEditorForm(JsonDataEditor jsonDataEditor) {
        super(jsonDataEditor, jsonDataEditor.getProject());
        this.jsonDataEditor = WeakRef.of(jsonDataEditor);

        DBJsonView jsonView = getJsonView();
        try {
            toolbarPanel.setBorder(Borders.insetBorder(2));

            tablePanel.setBorder(Borders.tableBorder(1, 0, 0, 0));
            editorPanel.setBorder(Borders.tableBorder(0, 1, 0, 0));
            editorPanel.setVisible(false);
            jsonDataEditorTable = new JsonDataEditorTable(this, jsonDataEditor);
            jsonDataTableScrollPane.setViewportView(jsonDataEditorTable);
            jsonDataEditorTable.initTableGutter();

            ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.JsonDataEditor");
            setAccessibleName(actionToolbar, txt("app.dataEditor.aria.JsonDataEditorActions"));

            actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
            loadingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
            hideLoadingHint();

            ActionToolbar loadingActionToolbar = Actions.createActionToolbar(actionsPanel, true, new CancelLoadingAction());
            loadingActionPanel.add(loadingActionToolbar.getComponent(), BorderLayout.CENTER);

            Disposer.register(this, autoCommitLabel);
        } catch (SQLException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(
                    getProject(),
                    txt("msg.dataEditor.title.FailedToOpenEditor"),
                    txt("msg.dataEditor.error.FailedToOpenEditor", jsonView.getQualifiedNameWithType(), e));
        }

        if (jsonView.isEditable(DBContentType.JSON)) {
            ConnectionHandler connection = getConnectionHandler();
            autoCommitLabel.init(getProject(), jsonDataEditor.getFile(), connection, SessionId.MAIN);
        }

        UserInterface.whenShown(mainPanel, () -> jsonDataEditorTable.requestFocus(), false);

        mainPanel.setFocusCycleRoot(true);
        mainPanel.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        mainPanel.setFocusTraversalPolicyProvider(true);
        searchPanel.setVisible(false);

        JsonDataContentEditorForm contentEditorForm = new JsonDataContentEditorForm(this);
        editorPanel.add(contentEditorForm.getComponent());
        this.contentEditorForm = WeakRef.of(contentEditorForm);

        setSplitPaneProportion(editorSplitPanel, 0.2);
    }

    public JsonDataEditorTable beforeRebuild() throws SQLException {
        JsonDataEditorTable oldEditorTable = getEditorTable();
        JsonDataEditor dataEditor = getJsonDataEditor();

        jsonDataEditorTable = new JsonDataEditorTable(this, dataEditor);
        return oldEditorTable;
    }

    public void afterRebuild(JsonDataEditorTable oldEditorTable) {
        if (isDisposed()) return;

        // update viewport and co. only if table was rebuilt (a.i. the old table is not null)
        if (oldEditorTable == null) return;
        dispatch(() -> {
            JsonDataEditorTable jsonDataEditorTable = getEditorTable();
            jsonDataTableScrollPane.setViewportView(jsonDataEditorTable);
            jsonDataEditorTable.initTableGutter();
            jsonDataEditorTable.updateBackground(false);

            Disposer.dispose(oldEditorTable);
        });
    }

    public void selectRecord(JsonDataEditorModelCell cell) {
        getContentEditorForm().selectRecord(cell);
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    public JsonDataContentEditorForm getContentEditorForm() {
        return WeakRef.ensure(contentEditorForm);
    }

    @NotNull
    public DBJsonView getJsonView() {
        return getJsonDataEditor().getJsonView();
    }

    @NotNull
    public JsonDataEditor getJsonDataEditor() {
        return jsonDataEditor.ensure();
    }

    public void showLoadingHint() {
        dispatch(() -> nn(loadingDataPanel).setVisible(true));
    }

    public void hideLoadingHint() {
        dispatch(() -> nn(loadingDataPanel).setVisible(false));
    }

    @NotNull
    public JsonDataEditorTable getEditorTable() {
        return nn(jsonDataEditorTable);
    }

    private ConnectionHandler getConnectionHandler() {
        return getEditorTable().getJsonView().getConnection();
    }

    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/

    @Override
    public @NotNull JPanel getSearchPanel() {
        return searchPanel;
    }

    @Override
    public void showSearchHeader() {
        getJsonDataEditor().setContentEditorVisible(false);
        JsonDataEditorTable editorTable = getEditorTable();
        editorTable.clearSelection();

        DataSearchComponent dataSearchComponent = getSearchComponent();
        dataSearchComponent.initializeFindModel();

        JTextComponent searchField = dataSearchComponent.getSearchField();
        if (searchPanel.isVisible()) {
            searchField.selectAll();
        } else {
            searchPanel.setVisible(true);    
        }
        dispatch(() -> searchField.requestFocus());
    }

    @Override
    public void hideSearchHeader() {
        if (!searchPanel.isVisible()) return;

        getSearchComponent().resetFindModel();
        searchPanel.setVisible(false);
        JsonDataEditorTable editorTable = getEditorTable();
        UserInterface.repaintAndFocus(editorTable);
    }

    @Override
    public void cancelEditActions() {
    }

    @Override
    public String getSelectedText() {
        return null;
    }

    @NotNull
    @Override
    public BasicTable<?> getTable() {
        return getEditorTable();
    }

    public boolean isContentEditorVisible() {
        return editorPanel.isVisible();
    }

    public void setContentEditorVisible(boolean visible) {
        editorPanel.setVisible(visible);
        if (visible) hideSearchHeader();
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
        if (DataKeys.JSON_DATA_EDITOR.is(dataId)) return getJsonDataEditor();
        return null;
    }
}
