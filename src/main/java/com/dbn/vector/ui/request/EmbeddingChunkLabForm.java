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
import com.dbn.common.file.FileTypes;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.request.EmbeddingChunkingConfig;
import com.intellij.openapi.Disposable;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import java.awt.BorderLayout;
import java.sql.ResultSet;

import static com.dbn.common.ui.util.Buttons.onButtonClick;
import static com.dbn.common.util.Editors.updateEditorScrollPane;

public class EmbeddingChunkLabForm extends DBNFormBase {

    private JPanel mainPanel;
    private ResultSetTable chunkDataTable;
    private JButton testButton;
    private DBNScrollPane outputScrollPane;
    private JComboBox<String> chunkByComboBox;
    private JComboBox<String> splitByComboBox;
    private JSpinner maxSpinner;
    private JSpinner overlapSpinner;
    private JPanel spinPanel;
    private JPanel outputPanel;
    private JPanel hintPanel;
    private JPanel inputPanel;
    private final ConnectionRef connection;

    private EditorEx inputEditor;
    private int inputLineCount;

    public EmbeddingChunkLabForm(@Nullable Disposable parent, ConnectionHandler connection, EmbeddingChunkingConfig config) {
        super(parent, connection.getProject());
        this.connection = connection.ref();

        initHintPanel();
        initOutputPanel();
        initConfigFields(config);
        initInputTextArea();
        initSpinner();
        initTestButton();
    }

    private void initHintPanel() {
        TextContent textContent = TextContent.plain(
                "Use this tool to experiment with different chunking settings before applying them in embedding and retrieval workflows. " +
                        "Adjust the parameters, preview the resulting chunks, and fine-tune the configuration that works best for your data.");
        DBNHintForm hintForm = new DBNHintForm(this, textContent, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initOutputPanel() {
        ConnectionHandler connection = getConnection();
        RecordViewInfo recordViewInfo = new RecordViewInfo("Chunk data", null);
        ResultSetDataModel dataModel = new ResultSetDataModel<>(connection);
        chunkDataTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        outputScrollPane.setViewportView(chunkDataTable);
        chunkDataTable.installValuePopupAddon();
        outputPanel.setBorder(Borders.lineBorder(Colors.getOutlineColor()));
    }

    private void initInputTextArea() {
        Project project = ensureProject();
        String text = "";
        PlainTextFileType fileType = FileTypes.getTextFileType();
        VirtualFile virtualFile = new LightVirtualFile("chunk_lab_input.txt", fileType, text);

        Document document = Documents.createDocument(text);
        document.addDocumentListener(formLayoutUpdater(), this);
        inputEditor = Editors.createEditor(document, project, virtualFile, fileType);
        inputEditor.setEmbeddedIntoDialogWrapper(false);
        inputEditor.getContentComponent().setFocusTraversalKeysEnabled(false);
        inputEditor.setPlaceholder("Enter your sample text for chunking here");
        inputEditor.setBorder(null);
        inputEditor.getComponent().setBorder(null);
        updateEditorScrollPane(inputEditor);

        EditorSettings settings = inputEditor.getSettings();
        settings.setUseSoftWraps(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineNumbersShown(false);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        settings.setAdditionalLinesCount(2);


        inputPanel.add(inputEditor.getComponent());
    }

    private void initSpinner() {
        spinPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
        spinPanel.setVisible(false);
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

    private ConnectionHandler getConnection() {
        return connection.ensure();
    }

    private void initConfigFields(EmbeddingChunkingConfig chunkConfig) {
        chunkByComboBox.setSelectedItem(chunkConfig.getChunkBy());
        splitByComboBox.setSelectedItem(chunkConfig.getSplitBy());
        maxSpinner.setValue(chunkConfig.getMaxSize());
        overlapSpinner.setValue(chunkConfig.getOverlap());
    }

    private void initTestButton() {
        onButtonClick(testButton, e ->
                Dispatch.async(mainPanel,
                        () -> chunkTextContent(),
                        d -> applyChunkResult(d)));
    }

    private ResultSetDataModel chunkTextContent() {
        startActivityNotifier();
        // todo recheck
        String query = inputEditor.getDocument().getText().replace("'", ""); // TODO prepared statement param binding

        EmbeddingChunkingConfig configuration = getChunkConfiguration();
        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();

        try {
            DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
            ResultSet resultSet = vectorManager.chunkTextContent(connection, configuration, query);
            ResultSetDataModel dataModel = new ResultSetDataModel((DBNResultSet) resultSet, connection, -1);
            dataModel.fetchNextRecords(1000, false);
            return dataModel;
        } catch (Exception e) {
            Messages.showErrorDialog(project, "Failed to chunk data", e);
            return new ResultSetDataModel(connection);
        } finally {
            stopActivityNotifier();
        }
    }

    private void applyChunkResult(ResultSetDataModel chunkData) {
        chunkDataTable.setModel(chunkData);
    }

    private void startActivityNotifier() {
        spinPanel.setVisible(true);
        testButton.setEnabled(false);
        chunkDataTable.setLoading(true);
    }

    /**
     * Stops the spining wheel
     */
    private void stopActivityNotifier() {
        spinPanel.setVisible(false);
        testButton.setEnabled(true);
        chunkDataTable.setLoading(false);
    }

    public EmbeddingChunkingConfig getChunkConfiguration() {
        String by = (String) chunkByComboBox.getSelectedItem();
        int max = (int) maxSpinner.getValue();
        String splitBy = (String) splitByComboBox.getSelectedItem();
        int overlap = (int) overlapSpinner.getValue();

        return new EmbeddingChunkingConfig(by, max, splitBy, overlap);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(inputEditor);
        super.disposeInner();
    }
}
