/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.editor.session.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.JLabel;

public class SessionBrowserErrorNotificationPanel extends EditorNotificationPanel{
    protected final JLabel label = new JLabel();

    public SessionBrowserErrorNotificationPanel(Project project, VirtualFile file, String error) {
        super(project, file, MessageType.ERROR);
        ConnectionHandler connection = getConnection();
        setText("Could not load sessions for " + connection.getName() + ". Error details: " + error.replace("\n", " "));
    }
}
