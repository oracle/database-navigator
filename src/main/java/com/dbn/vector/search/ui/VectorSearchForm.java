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
import com.dbn.common.file.FileTypes;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.type.DBVectorDistanceMetric;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.search.VectorSearchConsole;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.ResultSet;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Buttons.onButtonClick;
import static com.dbn.common.util.Editors.updateEditorScrollPane;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.help.HelpTopic.VECTOR_SEARCH;

public class VectorSearchForm extends DBNFormBase {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel inputPanel;
    private JButton searchButton;
    private JPanel spinPanel;
    private JPanel resultPanel;

    private final WeakRef<VectorSearchConsole> searchConsole;
    private VectorSearchResultForm resultForm;
    private EditorEx requestEditor;
    private int inputLineCount;

    @Getter
    private transient boolean searching;

    public VectorSearchForm(VectorSearchConsole searchConsole) {
        super(searchConsole, searchConsole.getProject());
        this.searchConsole = WeakRef.of(searchConsole);

        initActionToolbar();
        initSpinner();
        initSearchButton();
        initResultForm();
        initRequestEditor();
    }

    private void initActionToolbar() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.VectorSearchConsole");
        setAccessibleName(actionToolbar, txt("app.vectors.aria.VectorSearchActions"));

        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
        DataProviders.register(actionsPanel, this);
    }

    private void initRequestEditor() {
        Project project = ensureProject();
        String text = getSearchConsole().getConsoleFile().getContentText();
        PlainTextFileType fileType = FileTypes.getTextFileType();
        VirtualFile virtualFile = new LightVirtualFile("vector_search_file.txt", fileType, text);

        Document document = Documents.createDocument(text);
        document.addDocumentListener(documentChangeListener(), this);

        requestEditor = Editors.createEditor(document, project, virtualFile, fileType);
        requestEditor.setEmbeddedIntoDialogWrapper(false);
        requestEditor.getContentComponent().setFocusTraversalKeysEnabled(false);
        requestEditor.setPlaceholder("Enter your search text here");
        updateEditorScrollPane(requestEditor);

        EditorSettings settings = requestEditor.getSettings();
        settings.setUseSoftWraps(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineNumbersShown(false);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        settings.setAdditionalLinesCount(2);

        inputPanel.add(requestEditor.getComponent());
    }

    private @NotNull DocumentListener documentChangeListener() {
        return new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                Document document = event.getDocument();
                getSearchConsole().getConsoleFile().setContent(document.getText());

                int lineCount = document.getLineCount();
                if (lineCount == inputLineCount) return;

                inputLineCount = lineCount;
                revalidateForm();
            }
        };
    }

    private void initSpinner() {
        spinPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
        spinPanel.setVisible(false);
    }

    private void initResultForm() {
        resultForm = new VectorSearchResultForm(this);
        resultPanel.add(resultForm.getComponent());
    }

    private void initSearchButton() {
        onButtonClick(searchButton, e ->
                Dispatch.async(mainPanel,
                    () -> performSimilaritySearch(),
                    d -> applySearchResult(d)));
    }

    private ResultSetDataModel performSimilaritySearch() {
        VectorSearchConsole searchConsole = getSearchConsole();
        ConnectionHandler connection = searchConsole.getConnection();
        Project project = connection.getProject();

        DBSchema selectedSchema = searchConsole.getSelectedSchema();
        if (selectedSchema == null) {
            showErrorDialog(project, "No Schema Selection", "Please select a schema and a vector table to perform the similarity search on.");
            return null;
        }

        DBTable selectedTable = searchConsole.getSelectedTable();
        if (selectedTable == null) {
            showErrorDialog(project, "No Table Selection", "Please select a vector table to perform the similarity search on.");
            return null;
        }

        DBVectorDistanceMetric distanceMetric = searchConsole.getSelectedMetric();
        if (distanceMetric == null) {
            showErrorDialog(project, "No Metric Selection", "Please select a vector distance metric to use in the similarity search.");
            return null;
        }

        String query = requestEditor.getDocument().getText().trim();
        if (Strings.isEmptyOrSpaces(query)) {
            showErrorDialog(project, "Empty Query", "Please enter a query text to perform the similarity search for.");
            return null;
        }

        startActivityNotifier();
        try {
            DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
            ResultSet resultSet = vectorManager.performSimilaritySearch(connection, selectedTable, query, distanceMetric, 10);
            ResultSetDataModel dataModel = new ResultSetDataModel((DBNResultSet) resultSet, connection, -1);
            dataModel.fetchNextRecords(1000, false);
            return dataModel;
        } catch (Exception e) {
            showErrorDialog(project, "Failed to perform similarity search", e);
            return new ResultSetDataModel(connection);
        } finally {
            stopActivityNotifier();
        }
    }

    private void applySearchResult(ResultSetDataModel result){
        resultForm.setSearchResult(result);
    }

    private void startActivityNotifier() {
        searching = true;
        spinPanel.setVisible(true);
        searchButton.setEnabled(false);

        resultForm.setLoading(true);
        updateActionToolbars();
    }

    private void stopActivityNotifier() {
        checkDisposed();
        searching = false;
        spinPanel.setVisible(false);
        searchButton.setEnabled(true);

        resultForm.setLoading(false);
        updateActionToolbars();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    public ResultSetTable getSearchResultTable() {
        return resultForm.getResultTable();
    }

    @NotNull
    public VectorSearchConsole getSearchConsole() {
        return searchConsole.ensure();
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
