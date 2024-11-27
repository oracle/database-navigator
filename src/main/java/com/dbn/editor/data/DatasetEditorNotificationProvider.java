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

package com.dbn.editor.data;

import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.editor.data.ui.DatasetEditorLoadErrorNotificationPanel;
import com.dbn.editor.data.ui.DatasetEditorNotificationPanel;
import com.dbn.editor.data.ui.DatasetEditorReadonlyNotificationPanel;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBDatasetVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatasetEditorNotificationProvider extends EditorNotificationProvider<DatasetEditorNotificationPanel> {
    private static final Key<DatasetEditorNotificationPanel> KEY = Key.create("DBNavigator.DatasetEditorNotificationPanel");

    public DatasetEditorNotificationProvider() {
        ProjectEvents.subscribe(DatasetLoadListener.TOPIC, datasetLoadListener());
        ProjectEvents.subscribe(EnvironmentManagerListener.TOPIC, environmentManagerListener());
    }

    @NotNull
    private static DatasetLoadListener datasetLoadListener() {
        return new DatasetLoadListener() {
            @Override
            public void datasetLoaded(@NotNull DBVirtualFile virtualFile) {
                Project project = virtualFile.getProject();
                EditorNotifications notifications = Editors.getNotifications(project);
                notifications.updateNotifications((VirtualFile) virtualFile);
            }

            @Override
            public void datasetLoading(@NotNull DBVirtualFile virtualFile) {
                datasetLoaded(virtualFile);
            }
        };
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                updateEditorNotification(project, null);
            }

            @Override
            public void editModeChanged(Project project, DBContentVirtualFile databaseContentFile) {
                if (databaseContentFile instanceof DBDatasetVirtualFile) {
                    updateEditorNotification(project, databaseContentFile);
                }
            }
        };
    }

    @NotNull
    @Override
    public Key<DatasetEditorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public DatasetEditorNotificationPanel createComponent(@NotNull VirtualFile virtualFile, @NotNull FileEditor fileEditor, @NotNull Project project) {
        if (!(virtualFile instanceof DBEditableObjectVirtualFile)) return null;
        if (!(fileEditor instanceof DatasetEditor)) return null;

        DBEditableObjectVirtualFile editableObjectFile = (DBEditableObjectVirtualFile) virtualFile;
        DatasetEditor datasetEditor = (DatasetEditor) fileEditor;

        DBSchemaObject editableObject = editableObjectFile.getObject();
        if (!datasetEditor.isLoaded()) return null;

        String sourceLoadError = datasetEditor.getDataLoadError();
        if (Strings.isNotEmpty(sourceLoadError)) {
            return new DatasetEditorLoadErrorNotificationPanel(editableObject, sourceLoadError);
        }

        if (editableObject instanceof DBTable && editableObjectFile.getEnvironmentType().isReadonlyData()) {
            return new DatasetEditorReadonlyNotificationPanel(editableObject);
        }

        return null;
    }
}
