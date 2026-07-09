/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.common.util.Messages;
import com.dbn.diagnostics.ParserDiagnosticsManager;
import com.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Editors.updateNotifications;
import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.ParserIssueEditorNotificationProvider.markDismissed;
import static com.dbn.nls.NlsResources.txt;

public class ParserIssueEditorNotificationPanel extends EditorNotificationPanel {
    private final DBLanguagePsiFile psiFile;
    private final VirtualFile contentFile;

    public ParserIssueEditorNotificationPanel(@NotNull Project project, @NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull DBLanguagePsiFile psiFile) {
        super(project, file, fileEditor, MessageType.WARNING);
        this.psiFile = psiFile;
        this.contentFile = psiFile.getVirtualFile();
        setIcon(AllIcons.Actions.IntentionBulb);
        setText(txt("ntf.diagnostics.text.ParserIssue"));

        createActionLabel(txt("app.diagnostics.action.SubmitErrorReport"), this::submitReport);
        createActionLabel(txt("app.shared.action.Dismiss"), this::dismiss);
    }

    private void submitReport() {
        ParserDiagnosticsManager.get(getProject()).submitParserIssueReport(psiFile);
    }

    private void dismiss() {
        Project project = getProject();
        int option = Messages.showConfirmationDialog(
                project,
                txt("msg.diagnostics.title.DismissParserIssue"),
                txt("msg.diagnostics.question.DismissParserIssue"),
                options(
                        txt("app.diagnostics.action.DismissForFile"),
                        txt("app.diagnostics.action.DismissForProject"),
                        txt("msg.shared.button.Cancel")),
                0);

        if (option == 0) {
            markDismissed(contentFile);
            updateNotifications(project, getFile());
        } else if (option == 1) {
            CodeEditorGeneralSettings settings = CodeEditorGeneralSettings.get(project);
            settings.setShowParserIssueNotifications(false);
            updateNotifications(project, null);
        }
    }

}
