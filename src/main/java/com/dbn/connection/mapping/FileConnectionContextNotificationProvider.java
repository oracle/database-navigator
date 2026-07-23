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

package com.dbn.connection.mapping;

import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Dispatch;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusListener;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.ui.FileConnectionContextNotificationPanel;
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.dbn.common.util.Editors.getNotifications;
import static com.dbn.common.util.Editors.getOpenFiles;

public class FileConnectionContextNotificationProvider extends EditorNotificationProvider<FileConnectionContextNotificationPanel> {
    private static final Key<FileConnectionContextNotificationPanel> KEY = Key.create("DBNavigator.FileConnectionMappingNotificationPanel");
    public FileConnectionContextNotificationProvider() {
        ProjectEvents.subscribe(FileConnectionContextListener.TOPIC, createContextListener());
        ProjectEvents.subscribe(ConnectionHandlerStatusListener.TOPIC, createConnectionHandlerStatusListener());
    }

    @NotNull
    @Override
    public Key<FileConnectionContextNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public FileConnectionContextNotificationPanel createComponent(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {

        FileType fileType = file.getFileType();
        if (fileType == SQLFileType.INSTANCE || fileType == PSQLFileType.INSTANCE) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        FileConnectionContext mapping = contextManager.getMapping(file);
        if (mapping == null) return null;
        if (!mapping.isValid()) return null;

        return new FileConnectionContextNotificationPanel(project, file, fileEditor, mapping);
    }

    private static FileConnectionContextListener createContextListener() {
        return new FileConnectionContextListener() {
            @Override
            public void mappingChanged(Project project, VirtualFile file) {
/*
                // TODO cleanup - does not support inherited database context
                if (file instanceof VirtualFileWindow) {
                    VirtualFileWindow fileWindow = (VirtualFileWindow) file;
                    file = fileWindow.getDelegate();
                }

                EditorNotifications notifications = getNotifications(project);;
                FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
                VirtualFile[] openFiles = getOpenFiles(project);
                for (VirtualFile openFile : openFiles) {
                    FileConnectionContext mapping = contextManager.getMapping(openFile);
                    if (mapping == null) continue;

                    if (Objects.equals(mapping.getFile(), file)) {
                        notifications.updateNotifications(openFile);
                    }
                }
*/

                EditorNotifications notifications = getNotifications(project);;
                notifications.updateAllNotifications();

            }
        };
    }

    private static ConnectionHandlerStatusListener createConnectionHandlerStatusListener() {
        return connectionId -> Dispatch.run(true, () -> {
            ConnectionHandler connection = ConnectionHandler.get(connectionId);
            if(connection == null) return;

            Project project = connection.getProject();

            EditorNotifications notifications = getNotifications(project);
            VirtualFile[] openFiles = getOpenFiles(project);

            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            for (VirtualFile file : openFiles) {
                ConnectionId contextConnectionId = contextManager.getConnectionId(file);
                if (Objects.equals(contextConnectionId, connectionId)) {
                    notifications.updateNotifications(file);
                }
            }
        });
    }
}
