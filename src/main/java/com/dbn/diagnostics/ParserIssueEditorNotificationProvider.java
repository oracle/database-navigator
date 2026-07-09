/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics;

import com.dbn.common.editor.BasicTextEditor;
import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.diagnostics.ui.ParserIssueEditorNotificationPanel;
import com.dbn.editor.code.options.CodeEditorGeneralSettings;
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
    public static final Key<String> DISMISSED_CONTENT = Key.create("DBNavigator.ParserIssueNotificationDismissedContent");
    private static final Key<ParserIssueEditorNotificationPanel> KEY = Key.create("DBNavigator.ParserIssueEditorNotificationPanel");

    @Override
    public @NotNull Key<ParserIssueEditorNotificationPanel> getKey() {
        return KEY;
    }

    @Override
    public @Nullable ParserIssueEditorNotificationPanel createComponent(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        CodeEditorGeneralSettings settings = CodeEditorGeneralSettings.get(project);
        if (!settings.isShowParserIssueNotifications()) return null;

        VirtualFile relevantFile = fileEditor instanceof BasicTextEditor<?> bte ? bte.getVirtualFile() : file;
        if (isDismissed(relevantFile)) return null;

        PsiFile psiFile = PsiUtil.getPsiFile(project, relevantFile);
        if (!(psiFile instanceof DBLanguagePsiFile dbLanguagePsiFile)) return null;
        if (!PsiUtil.hasErrors(psiFile)) return null;

        return new ParserIssueEditorNotificationPanel(project, file, fileEditor, dbLanguagePsiFile);
    }

    public static boolean isDismissed(@NotNull VirtualFile file) {
        return contentVersion(file).equals(file.getUserData(DISMISSED_CONTENT));
    }

    public static void markDismissed(@NotNull VirtualFile file) {
        file.putUserData(DISMISSED_CONTENT, contentVersion(file));
    }

    private static String contentVersion(@NotNull VirtualFile file) {
        return file.getModificationStamp() + ":" + file.getLength();
    }
}
