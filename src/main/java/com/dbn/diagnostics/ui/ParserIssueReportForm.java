/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.diagnostics.ParserIssueReportInput;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

public class ParserIssueReportForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel codePanel;
    private JPanel commentPanel;

    private final ParserIssueReportInput input;
    private EditorEx codeEditor;
    private EditorEx commentEditor;

    public ParserIssueReportForm(@NotNull Disposable parent, @NotNull Project project, @NotNull ParserIssueReportInput input) {
        super(parent, project);
        this.input = input;

        initHeaderPanel();
        initCodeEditor();
        initCommentEditor();
    }

    private void initHeaderPanel() {
        hintPanel.add(new DBNHintForm(this,
                TextContent.plain(txt("app.diagnostics.hint.ParserIssue")), null, true).getComponent());
    }

    private void initCodeEditor() {
        Project project = ensureProject();

        DBLanguagePsiFile previewFile = DBLanguagePsiFile.createFromText(
                project, "parser-issue-preview." + input.getFileType().getDefaultExtension(), input.getLanguageDialect(),
                input.getCode(), null, null);
        if (previewFile == null) return;

        Document document = Documents.ensureDocument(previewFile);
        codeEditor = Editors.createEditor(document, project, previewFile.getVirtualFile(), input.getFileType());
        codeEditor.setEmbeddedIntoDialogWrapper(true);

        Editors.initEditorHighlighter(codeEditor, input.getLanguageDialect());
        Editors.updateEditorScrollPane(codeEditor);

        EditorSettings settings = codeEditor.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);

        Editors.installEditorLayoutUpdater(codeEditor, this);
        codePanel.add(codeEditor.getComponent());
    }

    private void initCommentEditor() {
        Project project = ensureProject();
        Document document = Documents.createDocument("");
        commentEditor = Editors.createEditor(document, project, null, PlainTextFileType.INSTANCE);
        commentEditor.setEmbeddedIntoDialogWrapper(true);
        commentEditor.setPlaceholder(txt("app.diagnostics.placeholder.ParserIssueComment"));

        EditorSettings settings = commentEditor.getSettings();
        settings.setUseSoftWraps(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineNumbersShown(false);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(1);

        Editors.updateEditorScrollPane(commentEditor);
        Editors.installEditorLayoutUpdater(commentEditor, this);
        Editors.restrictEditorHeight(commentEditor, this, 120);
        commentPanel.add(commentEditor.getComponent());
    }

    @Override
    public void applyFormChanges() {
        String code = codeEditor == null ? input.getCode() : codeEditor.getDocument().getText();
        String comment = commentEditor == null ? null : commentEditor.getDocument().getText();
        input.update(code, comment);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(codeEditor);
        Editors.releaseEditor(commentEditor);
        super.disposeInner();
    }
}
