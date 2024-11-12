package com.dbn.editor.session;

import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.session.ui.SessionBrowserErrorNotificationPanel;
import com.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SessionBrowserNotificationProvider extends EditorNotificationProvider<SessionBrowserErrorNotificationPanel> {
    private static final Key<SessionBrowserErrorNotificationPanel> KEY = Key.create("DBNavigator.SessionBrowserErrorNotificationPanel");

    public SessionBrowserNotificationProvider() {
        ProjectEvents.subscribe(SessionBrowserLoadListener.TOPIC, sessionBrowserLoadListener());
    }

    @NotNull
    private static SessionBrowserLoadListener sessionBrowserLoadListener() {
        return virtualFile -> {
            if (virtualFile instanceof DBSessionBrowserVirtualFile) {
                DBSessionBrowserVirtualFile databaseFile = (DBSessionBrowserVirtualFile) virtualFile;
                Project project = databaseFile.getProject();
                EditorNotifications notifications = Editors.getNotifications(project);;
                notifications.updateNotifications(virtualFile);
            }
        };
    }

    @NotNull
    @Override
    public Key<SessionBrowserErrorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public SessionBrowserErrorNotificationPanel createComponent(@NotNull VirtualFile virtualFile, @NotNull FileEditor fileEditor, @NotNull Project project) {
        if (!(virtualFile instanceof DBSessionBrowserVirtualFile)) return null;
        if (!(fileEditor instanceof SessionBrowser)) return null;

        SessionBrowser sessionBrowser = (SessionBrowser) fileEditor;
        ConnectionHandler connection = sessionBrowser.getConnection();
        String error = sessionBrowser.getModelError();
        if (Strings.isEmpty(error)) return null;

        return new SessionBrowserErrorNotificationPanel(project, virtualFile, error);
    }
}
