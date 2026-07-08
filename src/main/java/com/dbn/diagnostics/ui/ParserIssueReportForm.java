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
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

public class ParserIssueReportForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel codePanel;

    private final String code;
    private final FileType fileType;
    private final DBLanguageDialect languageDialect;

    private EditorEx viewer;

    public ParserIssueReportForm(@NotNull Disposable parent, @NotNull Project project, @NotNull String scrambledCode, @NotNull FileType fileType, @NotNull DBLanguageDialect languageDialect) {
        super(parent, project);
        this.code = scrambledCode;
        this.fileType = fileType;
        this.languageDialect = languageDialect;

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
                project, "parser-issue-preview." + fileType.getDefaultExtension(), languageDialect,
                code, null, null);
        if (previewFile == null) return;

        Document document = Documents.ensureDocument(previewFile);
        viewer = Viewers.createViewer(document, project, previewFile.getVirtualFile(), fileType);
        viewer.setEmbeddedIntoDialogWrapper(true);

        Editors.initEditorHighlighter(viewer, languageDialect);
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
