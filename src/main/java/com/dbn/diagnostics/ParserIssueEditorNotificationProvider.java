/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics;

import com.dbn.common.editor.BasicTextEditor;
import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.diagnostics.ui.ParserIssueEditorNotificationPanel;
import com.dbn.editor.code.options.CodeEditorSettings;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParserIssueEditorNotificationProvider extends EditorNotificationProvider<ParserIssueEditorNotificationPanel> {
    public static final Key<Boolean> DISMISSED = Key.create("DBNavigator.ParserIssueNotificationDismissed");
    private static final Key<ParserIssueEditorNotificationPanel> KEY = Key.create("DBNavigator.ParserIssueEditorNotificationPanel");

    @Override
    public @NotNull Key<ParserIssueEditorNotificationPanel> getKey() {
        return KEY;
    }

    @Override
    public @Nullable ParserIssueEditorNotificationPanel createComponent(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        if (Boolean.TRUE.equals(file.getUserData(DISMISSED))) return null;
        if (!CodeEditorSettings.getInstance(project).getGeneralSettings().isShowParserIssueNotifications()) return null;

        VirtualFile psiFileVirtualFile = fileEditor instanceof BasicTextEditor<?> basicTextEditor ?
                basicTextEditor.getVirtualFile() : file;

        PsiFile psiFile = PsiUtil.getPsiFile(project, psiFileVirtualFile);
        if (!(psiFile instanceof DBLanguagePsiFile dbLanguagePsiFile)) return null;
        if (!PsiUtil.hasErrors(psiFile)) return null;

        return new ParserIssueEditorNotificationPanel(project, file, fileEditor, dbLanguagePsiFile);
    }
}
