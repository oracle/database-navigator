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

package com.dbn.vector.search.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.action.DataProviders;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.file.FileTypes;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.search.VectorSearchConsole;
import com.dbn.vector.search.VectorSearchConsoleState;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.sql.ResultSet;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Splitters.setSplitPaneProportion;
import static com.dbn.help.HelpTopic.VECTOR_SEARCH;

public class VectorSearchForm extends DBNFormBase {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JSplitPane contentSplitPanel;
    private DBNScrollPane resultScrollPane;
    private JPanel requestInputPanel;
    private JButton searchButton;
    private JPanel spinPanel;
    private ResultSetTable searchResultTable;

    private final WeakRef<VectorSearchConsole> searchConsole;
    private EditorEx requestEditor;

    public VectorSearchForm(VectorSearchConsole searchConsole) {
        super(searchConsole, searchConsole.getProject());
        this.searchConsole = WeakRef.of(searchConsole);
        //contentPanel.setBorder(Borders.tableBorder(1, 0, 0, 0));
        setSplitPaneProportion(contentSplitPanel, 0.2);

        initActionToolbar();
        initSpinner();
        initSearchButton();
        initResultTable();
        initRequestEditor();


        Disposer.register(this, searchResultTable);
    }

    private void initActionToolbar() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.VectorSearchConsole");
        setAccessibleName(actionToolbar, txt("app.vectors.aria.VectorSearchActions"));

        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
        DataProviders.register(actionsPanel, this);
    }

    private void initRequestEditor() {
        Project project = ensureProject();
        String text = "";
        PlainTextFileType fileType = FileTypes.getTextFileType();
        VirtualFile virtualFile = new LightVirtualFile("vector_search_file.txt", fileType, text);

        Document document = Documents.createDocument(text);
        document.addDocumentListener(createRequestListener(), this);
        requestEditor = Editors.createEditor(document, project, virtualFile, fileType);
        requestEditor.setEmbeddedIntoDialogWrapper(false);
        requestEditor.getContentComponent().setFocusTraversalKeysEnabled(false);
        requestEditor.setPlaceholder("Enter your search text here");
        EditorSettings settings = requestEditor.getSettings();
        settings.setUseSoftWraps(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineNumbersShown(false);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        settings.setAdditionalLinesCount(2);

        requestInputPanel.add(requestEditor.getComponent());
        requestInputPanel.setBorder(Borders.insetBorder(8, 8, 8, 8));
        requestInputPanel.setBackground(requestEditor.getBackgroundColor());
    }

    private DocumentListener createRequestListener() {
        return new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                searchButton.setEnabled(!event.getDocument().getText().isEmpty());
            }
        };
    }

    private void initSpinner() {
        spinPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
        spinPanel.setVisible(false);
    }

    private void initResultTable() {
        VectorSearchConsole searchEditor = getSearchConsole();
        RecordViewInfo recordViewInfo = new RecordViewInfo("Search result", null);
        ConnectionHandler connection = searchEditor.getConnection();
        ResultSetDataModel dataModel = new ResultSetDataModel<>(connection);
        searchResultTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        resultScrollPane.setViewportView(searchResultTable);
    }

    private void initSearchButton() {
        searchButton.addActionListener(e -> {
            Dispatch.async(mainPanel,
                    () -> performSimilaritySearch(),
                    d -> applyChunkResult(d));
        });
    }

    private ResultSetDataModel performSimilaritySearch() {
        startActivityNotifier();
        String query = requestEditor.getDocument().getText().trim();

        ConnectionHandler connection = getSearchConsole().getConnection();
        Project project = connection.getProject();

        DBSchema schema = connection.getSchema(getSelectedSchema());
        DBObjectRef<DBTable> tableRef = new DBObjectRef<>(schema.ref(), DBObjectType.TABLE, "DCIE_0001");
        DBTable vectorTable = tableRef.get();

        try {
            DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
            ResultSet resultSet = vectorManager.performSimilaritySearch(connection, vectorTable, query);
            ResultSetDataModel dataModel = new ResultSetDataModel((DBNResultSet) resultSet, connection, -1);
            dataModel.fetchNextRecords(1000, false);
            return dataModel;
        } catch (Exception e) {
            Messages.showErrorDialog(project, "Failed to perform similarity search", e);
            return new ResultSetDataModel(connection);
        } finally {
            stopActivityNotifier();
        }
    }

    private SchemaId getSelectedSchema() {
        Project project = ensureProject();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        return contextManager.getDatabaseSchema(getSearchConsole().getDatabaseFile());
    }

    private void applyChunkResult(ResultSetDataModel chunkData){
        searchResultTable.setModel(chunkData);
    }

    private void startActivityNotifier() {
        spinPanel.setVisible(true);
        searchButton.setEnabled(false);
    }

    private void stopActivityNotifier() {
        spinPanel.setVisible(false);
        searchButton.setEnabled(true);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    public ResultSetTable getSearchResultTable() {
        return Failsafe.nn(searchResultTable);
    }

    @NotNull
    public VectorSearchConsole getSearchConsole() {
        return searchConsole.ensure();
    }

    @NotNull
    private ConnectionHandler getConnectionHandler() {
        return getSearchConsole().getConnection();
    }

    public void updateState(VectorSearchConsoleState state) {
        DocumentEx document = requestEditor.getDocument();
        String searchText = Documents.getText(document);

        state.setSearchText(searchText);
    }

    public void applyState(VectorSearchConsoleState state) {
        DocumentEx document = requestEditor.getDocument();
        String searchText = state.getSearchText();


        Documents.setText(document, searchText);
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.VECTOR_SEARCH_CONSOLE.is(dataId)) return getSearchConsole();
        if (PlatformCoreDataKeys.HELP_ID.is(dataId)) return VECTOR_SEARCH.asHelpTopicId();
        return null;
    }

    @Override
    public void disposeInner() {
        DataProviders.unregister(actionsPanel);
        Editors.releaseEditor(requestEditor);
        super.disposeInner();
    }
}
