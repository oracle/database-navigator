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

package com.dbn.execution.statement.result.ui;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Viewers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Color;

import static com.dbn.nls.NlsResources.txt;

public class PendingTransactionDialogForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel previewPanel;
    private JPanel headerPanel;
    private JTextPane hintTextPane;

    private final WeakRef<StatementExecutionProcessor> executionProcessor;
    private EditorEx viewer;

    public PendingTransactionDialogForm(PendingTransactionDialog parent, final StatementExecutionProcessor executionProcessor) {
        super(parent);
        this.executionProcessor = WeakRef.of(executionProcessor);

        String text = txt("msg.execution.text.PendingTransactionPrompt");
        hintTextPane.setBackground(mainPanel.getBackground());
        hintTextPane.setText(text);

        String headerName = executionProcessor.getName();
        Icon headerIcon = executionProcessor.getIcon();

        DBLanguagePsiFile psiFile = executionProcessor.getPsiFile();
        Color headerColor = psiFile == null ?
                EnvironmentType.DEFAULT.getColor() :
                psiFile.getEnvironmentType().getColor();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, headerName, headerIcon, headerColor);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        updatePreview();
    }

    @NotNull
    public StatementExecutionProcessor getExecutionProcessor() {
        return executionProcessor.ensure();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }

    private void updatePreview() {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();

        ConnectionHandler connection = Failsafe.nn(executionProcessor.getConnection());
        SchemaId currentSchema = executionProcessor.getTargetSchema();
        Project project = connection.getProject();
        String previewText = executionProcessor.getExecutionInput().getPreviewStatementText();

        DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
        DBLanguagePsiFile selectStatementFile = DBLanguagePsiFile.createFromText(
                project,
                "preview.sql",
                languageDialect,
                previewText,
                connection,
                currentSchema);

        if (selectStatementFile == null) return;

        Document previewDocument = Documents.ensureDocument(selectStatementFile);
        viewer = Viewers.createViewer(previewDocument, project, null, SQLFileType.INSTANCE);
        viewer.setEmbeddedIntoDialogWrapper(true);
        Editors.updateEditorScrollPane(viewer);

        Editors.initEditorHighlighter(viewer, SQLLanguage.INSTANCE, connection);

        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        previewPanel.add(viewer.getComponent(), BorderLayout.CENTER);
    }


    @Override
    public void disposeInner() {
        Editors.releaseEditor(viewer);
        viewer = null;

        super.disposeInner();
    }
}
