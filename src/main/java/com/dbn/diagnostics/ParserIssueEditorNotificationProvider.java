/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics;

import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Editors;
import com.dbn.diagnostics.ui.ParserIssueEditorNotificationPanel;
import com.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ParserIssueEditorNotificationProvider extends EditorNotificationProvider<ParserIssueEditorNotificationPanel> {
    public static final Key<String> DISMISSED_CONTENT = Key.create("DBNavigator.ParserIssueNotificationDismissedContent");
    public static final Key<Boolean> NOTIFICATION_UPDATE_PENDING = Key.create("DBNavigator.ParserIssueNotificationUpdatePending");
    public static final Key<Boolean> NOTIFICATION_MUTED = Key.create("DBNavigator.ParserIssueNotificationMuted");
    private static final Key<ParserIssueEditorNotificationPanel> KEY = Key.create("DBNavigator.ParserIssueEditorNotificationPanel");

    public ParserIssueEditorNotificationProvider() {
        ProjectEvents.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
    }

    @NotNull
    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                for (FileEditor fileEditor : source.getEditors(file)) {
                    initializeNotificationState(source.getProject(), file, fileEditor);
                }
            }

            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
                FileEditor oldEditor = event.getOldEditor();
                FileEditor newEditor = event.getNewEditor();
                if (oldEditor == null || newEditor == null) return;
                if (oldEditor == newEditor && Objects.equals(event.getOldFile(), event.getNewFile())) return;

                revealNotification(event.getManager().getProject(), event, newEditor);
            }
        };
    }

    private static void initializeNotificationState(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull FileEditor fileEditor) {
        DBLanguagePsiFile databasePsiFile = getDatabasePsiFile(project, file, fileEditor);
        if (databasePsiFile == null) return;

        VirtualFile contentFile = databasePsiFile.getVirtualFile();
        DBLanguageDialect languageDialect = databasePsiFile.getLanguageDialect();
        if (languageDialect == null || !languageDialect.isInitialized() || !PsiUtil.hasErrors(databasePsiFile)) {
            mute(contentFile);
        } else {
            unmute(contentFile);
        }
    }

    private static void revealNotification(
            @NotNull Project project,
            @NotNull FileEditorManagerEvent event,
            @NotNull FileEditor fileEditor) {
        VirtualFile newFile = event.getNewFile();
        if (newFile == null) return;

        DBLanguagePsiFile databasePsiFile = getDatabasePsiFile(project, newFile, fileEditor);
        if (databasePsiFile == null) return;

        PsiDocumentManager.getInstance(project).performWhenAllCommitted(() -> {
            if (!fileEditor.isValid()) return;
            if (event.getManager().getSelectedEditor(newFile) != fileEditor) return;
            if (!PsiUtil.hasErrors(databasePsiFile)) return;

            unmute(databasePsiFile.getVirtualFile());
            Editors.updateNotifications(project, newFile);
        });
    }

    @Nullable
    private static DBLanguagePsiFile getDatabasePsiFile(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull FileEditor fileEditor) {
        Editor editor = Editors.getEditor(fileEditor);
        PsiFile psiFile = editor == null ? PsiUtil.getPsiFile(project, file) : PsiUtil.getPsiFile(editor);
        return psiFile instanceof DBLanguagePsiFile databasePsiFile ? databasePsiFile : null;
    }

    @Override
    public @NotNull Key<ParserIssueEditorNotificationPanel> getKey() {
        return KEY;
    }

    @Override
    public @Nullable ParserIssueEditorNotificationPanel createComponent(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        CodeEditorGeneralSettings settings = CodeEditorGeneralSettings.get(project);
        if (!settings.isShowParserIssueNotifications()) return null;

        DBLanguagePsiFile databasePsiFile = getDatabasePsiFile(project, file, fileEditor);
        if (databasePsiFile == null) return null;

        VirtualFile contentFile = databasePsiFile.getVirtualFile();
        if (isDismissed(contentFile)) return null;
        if (isMuted(contentFile)) return null;

        DBLanguageDialect languageDialect = databasePsiFile.getLanguageDialect();
        if (languageDialect == null) return null;
        if (!languageDialect.isInitialized()) return null;

        if (!PsiUtil.hasErrors(databasePsiFile)) return null;

        return new ParserIssueEditorNotificationPanel(project, file, fileEditor, databasePsiFile);
    }

    public static boolean isDismissed(@NotNull VirtualFile file) {
        return contentVersion(file).equals(file.getUserData(DISMISSED_CONTENT));
    }

    public static void markDismissed(@NotNull VirtualFile file) {
        file.putUserData(DISMISSED_CONTENT, contentVersion(file));
    }

    public static boolean isMuted(@NotNull VirtualFile file) {
        return Boolean.TRUE.equals(file.getUserData(NOTIFICATION_MUTED));
    }

    public static void mute(@NotNull VirtualFile file) {
        file.putUserData(NOTIFICATION_MUTED, true);
    }

    public static void unmute(@NotNull VirtualFile file) {
        file.putUserData(NOTIFICATION_MUTED, null);
    }

    private static String contentVersion(@NotNull VirtualFile file) {
        return file.getModificationStamp() + ":" + file.getLength();
    }
}
