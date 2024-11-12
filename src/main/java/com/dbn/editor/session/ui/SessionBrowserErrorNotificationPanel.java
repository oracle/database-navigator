package com.dbn.editor.session.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;

public class SessionBrowserErrorNotificationPanel extends EditorNotificationPanel{
    protected final JLabel label = new JLabel();

    public SessionBrowserErrorNotificationPanel(Project project, VirtualFile file, String error) {
        super(project, file, MessageType.ERROR);
        ConnectionHandler connection = getConnection();
        setText("Could not load sessions for " + connection.getName() + ". Error details: " + error.replace("\n", " "));
    }
}
