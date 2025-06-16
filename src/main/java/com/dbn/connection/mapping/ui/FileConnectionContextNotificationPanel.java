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

package com.dbn.connection.mapping.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Conditional.when;

public class FileConnectionContextNotificationPanel extends EditorNotificationPanel {
    private final boolean inheritedContext;

    public FileConnectionContextNotificationPanel(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull FileEditor fileEditor,
            @NotNull FileConnectionContext mapping) {
        super(project, file, fileEditor, MessageType.SYSTEM);

        ConnectionId connectionId = mapping.getConnectionId();
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection != null) {
            setText(connection.getName());
            setIcon(connection.getIcon());
        } else {
            setText("No connection selected");
            setIcon(null);
        }

        VirtualFile mappingFile = mapping.getFile();
        inheritedContext = mappingFile != null && !file.equals(mappingFile);
        if (inheritedContext) {
            JLabel inheritedLabel = new JLabel("(database context inherited from " + mappingFile.getPath() + ")");
            inheritedLabel.setForeground(UIUtil.getLabelDisabledForeground());
            inheritedLabel.setOpaque(false);
            setContent(inheritedLabel);
        }

        createActionLabel("Delink", () -> delink());
        createActionLabel("Mappings", () -> mappings());
    }

    private void delink() {
        Project project = getProject();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        VirtualFile file = getFile();
        if (inheritedContext) {
            FileConnectionContext mapping = contextManager.getMapping(file);
            if (mapping == null) return;
            if (!mapping.isValid()) return;

            VirtualFile mappingFile = nd(mapping.getFile());
            ConnectionHandler connection = nd(mapping.getConnection());

            Messages.showQuestionDialog(
                    project,
                    "Remove database context",
                    "Are you sure you want to delink the database \"" + connection.getName() + "\" from the directory \"" + mappingFile.getPath() + "\"?",
                    Messages.OPTIONS_YES_NO,
                    0,
                    option -> when(option == 0,
                            () -> contextManager.removeMapping(mappingFile)));



        } else {
            contextManager.removeMapping(file);
        }
    }

    private void mappings() {
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(getProject());
        FileConnectionContext selectedContext = contextManager.getMapping(getFile());
        contextManager.openFileConnectionMappings(selectedContext);
    }
}
