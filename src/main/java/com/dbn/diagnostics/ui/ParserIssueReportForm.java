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
import com.dbn.common.util.Viewers;
import com.dbn.diagnostics.ParserIssueReportInput;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

public class ParserIssueReportForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel codePanel;

    private final ParserIssueReportInput input;
    private EditorEx viewer;

    public ParserIssueReportForm(@NotNull Disposable parent, @NotNull Project project, @NotNull ParserIssueReportInput input) {
        super(parent, project);
        this.input = input;

        initHeaderPanel();
        initCodeViewer();
    }

    private void initHeaderPanel() {
        hintPanel.add(new DBNHintForm(this,
                TextContent.plain(txt("app.diagnostics.hint.ParserIssue")), null, true).getComponent());
    }

    private void initCodeViewer() {
        Project project = ensureProject();

        DBLanguagePsiFile previewFile = DBLanguagePsiFile.createFromText(
                project, "parser-issue-preview." + input.getFileType().getDefaultExtension(), input.getLanguageDialect(),
                input.getCode(), null, null);
        if (previewFile == null) return;

        Document document = Documents.ensureDocument(previewFile);
        viewer = Viewers.createViewer(document, project, previewFile.getVirtualFile(), input.getFileType());
        viewer.setEmbeddedIntoDialogWrapper(true);

        Editors.initEditorHighlighter(viewer, input.getLanguageDialect());
        Editors.setEditorReadonly(viewer, true);
        Editors.updateEditorScrollPane(viewer);

        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        Editors.installEditorLayoutUpdater(viewer, this);
        codePanel.add(viewer.getComponent());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(viewer);
        super.disposeInner();
    }
}
