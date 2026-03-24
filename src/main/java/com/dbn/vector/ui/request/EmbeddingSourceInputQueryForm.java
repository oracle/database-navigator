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

package com.dbn.vector.ui.request;

import com.dbn.common.color.Colors;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.vector.model.request.EmbeddingSourceQuery;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.dbn.vfs.file.DBSingleQueryVirtualFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.SQLException;

import static com.dbn.common.ui.util.Buttons.onButtonClick;

public class EmbeddingSourceInputQueryForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel queryPanel;
    private JPanel outputPanel;
    private JButton verifyButton;
    private JPanel spinPanel;
    private DBNScrollPane outputScrollPane;

    private final ConnectionRef connection;
    private final EmbeddingSourceQuery config;
    private ResultSetTable outputTable;

    private Document document;
    private EditorEx editor;
    private String statement;
    private int inputLineCount;

    public EmbeddingSourceInputQueryForm(@NotNull Disposable parent, ConnectionHandler connection, EmbeddingSourceQuery config) {
        super(parent);
        this.connection = ConnectionRef.of(connection);
        this.config = config;

        initHeaderPanel();
        initSpinner();
        initVerifyButton();
        initOutputPanel();

        // delay the initialization of the query editor to force psi write action in the dialog modality state
        whenFirstShown(() -> initFilterEditor() );
    }

    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, getConnection());
        headerPanel.add(headerForm.getComponent());
    }

    private void initFilterEditor() {
        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();

        DBSingleQueryVirtualFile queryFile = new DBSingleQueryVirtualFile(connection, config.getSelectStatement());
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, queryFile, true);
        PsiFile queryPsiFile = queryFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);

        document = Documents.ensureDocument(queryPsiFile);
        document.addDocumentListener(formLayoutUpdater(), this);
        editor = Editors.createEditor(document, project, queryFile, SQLFileType.INSTANCE);
        Editors.initEditorHighlighter(editor, SQLLanguage.INSTANCE, connection);

        editor.setEmbeddedIntoDialogWrapper(true);
        Editors.updateEditorScrollPane(editor);

        EditorSettings settings = editor.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setCaretRowShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setUseTabCharacter(true);

        queryPanel.add(editor.getComponent());
        Editors.focusEditor(editor);
    }


    private @NotNull DocumentListener formLayoutUpdater() {
        return new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                int lineCount = event.getDocument().getLineCount();
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

    private void initOutputPanel() {
        ConnectionHandler connection = getConnection();
        RecordViewInfo recordViewInfo = new RecordViewInfo("Query data", null);
        ResultSetDataModel dataModel = new ResultSetDataModel<>(connection);
        outputTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        outputScrollPane.setViewportView(outputTable);
        outputTable.installValuePopupAddon();
        outputPanel.setBorder(Borders.lineBorder(Colors.getOutlineColor()));
        outputTable.setLoading(true);
    }

    private void initVerifyButton() {
        onButtonClick(verifyButton, e ->
                Dispatch.async(mainPanel,
                    () -> verifyQuery(),
                    d -> applyChunkResult(d)));
    }

    private void applyChunkResult(ResultSetDataModel data){
        outputTable.setModel(data);
    }

    private ResultSetDataModel verifyQuery() {
        startActivityNotifier();
        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();

        try {
            return executeStatement();
        } catch (Exception e) {
            Messages.showErrorDialog(project, "Failed to verify query", e);
            return new ResultSetDataModel(connection);
        } finally {
            stopActivityNotifier();
        }
    }

    private ResultSetDataModel executeStatement() throws SQLException {
        ConnectionHandler connection = getConnection();
        return PooledConnection.call(connection.createConnectionContext(), conn -> {
            DBNStatement statement = null;
            DBNResultSet resultSet = null;
            try {
                statement = conn.createStatement();
                statement.execute(getQuery());
                resultSet = statement.getResultSet();

                ResultSetDataModel dataModel = new ResultSetDataModel(resultSet, connection, -1);
                dataModel.fetchNextRecords(1000, false);

                return dataModel;
            } finally {
                Resources.close(resultSet);
                Resources.close(statement);
            }
        });
    }

    private void startActivityNotifier() {
        spinPanel.setVisible(true);
        verifyButton.setEnabled(false);
        outputTable.setLoading(true);
    }

    private void stopActivityNotifier() {
        spinPanel.setVisible(false);
        verifyButton.setEnabled(true);
        outputTable.setLoading(false);
    }

    @Override
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void resetFormChanges() {
        Documents.setText(document, config.getSelectStatement());
    }

    @Override
    public void applyFormChanges() {
        config.setSelectStatement(getQuery());
    }

    private String getQuery() {
        return document.getText();
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
        document = null;
        super.disposeInner();
    }
}
