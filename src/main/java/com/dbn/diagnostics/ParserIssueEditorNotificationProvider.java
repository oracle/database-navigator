/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics;

import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextListener;
import com.dbn.diagnostics.ui.DialectSuggestionEditorNotificationPanel;
import com.dbn.diagnostics.ui.ParserIssueEditorNotificationPanel;
import com.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.dialect.DBLanguageDialectCache;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Objects;

import static com.dbn.common.util.Documents.whenDocumentsCommitted;
import static com.dbn.common.util.Editors.updateEditorNotifications;
import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.hasIssues;
import static com.dbn.language.common.dialect.DBLanguageDialectCache.getDetectedDialect;
import static com.dbn.language.common.dialect.DBLanguageDialectCache.getSelectedDialect;

public class ParserIssueEditorNotificationProvider extends EditorNotificationProvider<JComponent> {
    private static final Key<String> REPORTING_NOTIFICATION_DISMISSED_VERSION = Key.create("DBNavigator.ReportingNotificationDismissedVersion");
    private static final Key<Boolean> REPORTING_NOTIFICATION_UPDATE_PENDING = Key.create("DBNavigator.ReportingNotificationUpdatePending");

    private static final Key<String> DIALECT_NOTIFICATION_DISMISSED_VERSION = Key.create("DBNavigator.DialectNotificationDismissedVersion");
    private static final Key<Boolean> DIALECT_NOTIFICATION_UPDATE_PENDING = Key.create("DBNavigator.DialectNotificationUpdatePending");
    private static final Key<Boolean> NOTIFICATION_MUTED = Key.create("DBNavigator.NotificationMuted");

    private static final Key<JComponent> KEY = Key.create("DBNavigator.ParserIssueEditorNotificationPanel");

    public ParserIssueEditorNotificationProvider() {
        ProjectEvents.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
        ProjectEvents.subscribe(FileConnectionContextListener.TOPIC, fileConnectionContextListener());
    }

    @NotNull
    private FileConnectionContextListener fileConnectionContextListener() {
        return new FileConnectionContextListener() {
            @Override
            public void connectionChanged(
                    @NotNull Project project,
                    @NotNull VirtualFile file,
                    @Nullable ConnectionHandler connection) {
                setNotificationMuted(file, false);
                updateEditorNotifications(project, file);
            }
        };
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
        resetNotificationMuteState(databasePsiFile);
    }

    private static void resetNotificationMuteState(@Nullable DBLanguagePsiFile databasePsiFile) {
        if (databasePsiFile == null) return;

        VirtualFile contentFile = databasePsiFile.getVirtualFile();
        setNotificationMuted(contentFile, false);
    }

    private static void revealNotification(
            @NotNull Project project,
            @NotNull FileEditorManagerEvent event,
            @NotNull FileEditor fileEditor) {
        VirtualFile newFile = event.getNewFile();
        if (newFile == null) return;

        DBLanguagePsiFile databasePsiFile = getDatabasePsiFile(project, newFile, fileEditor);
        if (databasePsiFile == null) return;

        whenDocumentsCommitted(project, () -> {
            if (!fileEditor.isValid()) return;
            if (event.getManager().getSelectedEditor(newFile) != fileEditor) return;

            resetNotificationMuteState(databasePsiFile);
            updateEditorNotifications(project, newFile);
        });
    }

    private static boolean canReportParserIssue(@NotNull DBLanguagePsiFile psiFile) {
        DBLanguageDialect languageDialect = psiFile.getLanguageDialect();
        return languageDialect != null && languageDialect.isInitialized() && PsiUtil.hasErrors(psiFile);
    }

    private static boolean canSuggestDialect(@NotNull DBLanguagePsiFile psiFile) {
        ConnectionHandler connection = psiFile.getConnection();
        if (connection != null && !connection.isVirtual()) return false;

        DBLanguageDialect languageDialect = psiFile.getLanguageDialect();
        return languageDialect != null && languageDialect.isInitialized() &&
                Read.call(psiFile, f -> hasIssues(f));
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
    public @NotNull Key<JComponent> getKey() {
        return KEY;
    }

    @Override
    public @Nullable JComponent createComponent(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        DBLanguagePsiFile psiFile = getDatabasePsiFile(project, file, fileEditor);
        if (psiFile == null) return null;

        CodeEditorGeneralSettings settings = CodeEditorGeneralSettings.get(project);

        // A dialect suggestion owns the notification slot while it is available or being resolved.
        JComponent notificationPanel = createDialectNotificationPanel(settings, fileEditor, file, psiFile);
        if (notificationPanel != null) return notificationPanel;

        if (isDialectNotificationPending(settings, psiFile)) return null;
        return createReportingNotificationPanel(settings, fileEditor, file, psiFile);
    }

    @Nullable
    private static JComponent createDialectNotificationPanel(
            @NotNull CodeEditorGeneralSettings settings,
            @NotNull FileEditor fileEditor,
            @NotNull VirtualFile file,
            @NotNull DBLanguagePsiFile psiFile) {

        Project project = psiFile.getProject();
        VirtualFile contentFile = psiFile.getVirtualFile();

        if (!settings.isShowDialectSuggestionNotifications()) return null;
        if (isNotificationMuted(contentFile)) return null;
        if (isDialectNotificationDismissed(contentFile)) return null;
        if (getSelectedDialect(psiFile) != null) return null;
        if (!canSuggestDialect(psiFile)) return null;

        DBLanguageDialect detectedDialect = getDetectedDialect(psiFile);
        if (detectedDialect == null) return null;
        if (detectedDialect == psiFile.getLanguageDialect()) return null;

        return new DialectSuggestionEditorNotificationPanel(project, file, fileEditor, psiFile, detectedDialect);
    }

    private static boolean isDialectNotificationPending(
            @NotNull CodeEditorGeneralSettings settings,
            @NotNull DBLanguagePsiFile psiFile) {
        VirtualFile contentFile = psiFile.getVirtualFile();

        if (!settings.isShowDialectSuggestionNotifications()) return false;
        if (isNotificationMuted(contentFile)) return false;
        if (isDialectNotificationDismissed(contentFile)) return false;
        if (!canSuggestDialect(psiFile)) return false;

        return DBLanguageDialectCache.isPending(psiFile);
    }

    @Nullable
    private static JComponent createReportingNotificationPanel(
            @NotNull CodeEditorGeneralSettings settings,
            @NotNull FileEditor fileEditor,
            @NotNull VirtualFile file,
            @NotNull DBLanguagePsiFile psiFile) {

        Project project = psiFile.getProject();
        VirtualFile contentFile = psiFile.getVirtualFile();

        if (!settings.isShowParserIssueNotifications()) return null;
        if (isReportingNotificationDismissed(contentFile)) return null;
        if (isNotificationMuted(contentFile)) return null;
        if (!canReportParserIssue(psiFile)) {
            setNotificationMuted(contentFile, true);
            return null;
        }

        DBLanguageDialect languageDialect = psiFile.getLanguageDialect();
        if (languageDialect == null) return null;

        return new ParserIssueEditorNotificationPanel(project, file, fileEditor, psiFile, languageDialect);
    }

    private static boolean isReportingNotificationDismissed(@NotNull VirtualFile file) {
        return contentVersion(file).equals(file.getUserData(REPORTING_NOTIFICATION_DISMISSED_VERSION));
    }

    private static boolean isDialectNotificationDismissed(@NotNull VirtualFile file) {
        return contentVersion(file).equals(file.getUserData(DIALECT_NOTIFICATION_DISMISSED_VERSION));
    }

    private static boolean isNotificationMuted(@NotNull VirtualFile file) {
        return Boolean.TRUE.equals(file.getUserData(NOTIFICATION_MUTED));
    }

    public static void dismissDialectNotification(@NotNull VirtualFile file) {
        file.putUserData(DIALECT_NOTIFICATION_DISMISSED_VERSION, contentVersion(file));
    }

    public static void dismissReportingNotification(@NotNull VirtualFile file) {
        file.putUserData(REPORTING_NOTIFICATION_DISMISSED_VERSION, contentVersion(file));
    }

    public static boolean isReportingNotificationUpdatePending(@NotNull VirtualFile file) {
        return Boolean.TRUE.equals(file.getUserData(REPORTING_NOTIFICATION_UPDATE_PENDING));
    }

    public static void setReportingNotificationUpdatePending(@NotNull VirtualFile file, boolean pending) {
        file.putUserData(REPORTING_NOTIFICATION_UPDATE_PENDING, pending ? Boolean.TRUE : null);
    }

    public static boolean isDialectNotificationUpdatePending(@NotNull VirtualFile file) {
        return Boolean.TRUE.equals(file.getUserData(DIALECT_NOTIFICATION_UPDATE_PENDING));
    }

    public static void setDialectNotificationUpdatePending(@NotNull VirtualFile file, boolean pending) {
        file.putUserData(DIALECT_NOTIFICATION_UPDATE_PENDING, pending ? Boolean.TRUE : null);
    }

    public static void setNotificationMuted(@NotNull VirtualFile file, boolean muted) {
        file.putUserData(NOTIFICATION_MUTED, muted ? Boolean.TRUE : null);
    }

    private static String contentVersion(@NotNull VirtualFile file) {
        return file.getModificationStamp() + ":" + file.getLength();
    }
}
