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

package com.dbn.editor.json;

import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.editor.data.DataLoadListener;
import com.dbn.editor.json.ui.JsonDataEditorLoadErrorNotificationPanel;
import com.dbn.editor.json.ui.JsonDataEditorNotificationPanel;
import com.dbn.editor.json.ui.JsonDataEditorReadonlyNotificationPanel;
import com.dbn.object.DBJsonView;
import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBJsonDataVirtualFile;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonDataEditorNotificationProvider extends EditorNotificationProvider<JsonDataEditorNotificationPanel> {
    private static final Key<JsonDataEditorNotificationPanel> KEY = Key.create("DBNavigator.JsonDataEditorNotificationPanel");

    public JsonDataEditorNotificationProvider() {
        ProjectEvents.subscribe(DataLoadListener.TOPIC, dataLoadListener());
        ProjectEvents.subscribe(EnvironmentManagerListener.TOPIC, environmentManagerListener());
    }

    @NotNull
    private static DataLoadListener dataLoadListener() {
        return new DataLoadListener() {
            @Override
            public void dataLoaded(@NotNull DBVirtualFile virtualFile) {
                Project project = virtualFile.getProject();
                EditorNotifications notifications = Editors.getNotifications(project);
                notifications.updateNotifications((VirtualFile) virtualFile);
            }

            @Override
            public void dataLoading(@NotNull DBVirtualFile virtualFile) {
                dataLoaded(virtualFile);
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
                if (databaseContentFile instanceof DBJsonDataVirtualFile) {
                    updateEditorNotification(project, databaseContentFile);
                }
            }
        };
    }

    @NotNull
    @Override
    public Key<JsonDataEditorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public JsonDataEditorNotificationPanel createComponent(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        if (!(file instanceof DBEditableObjectVirtualFile)) return null;
        if (!(fileEditor instanceof JsonDataEditor)) return null;

        DBEditableObjectVirtualFile editableObjectFile = (DBEditableObjectVirtualFile) file;
        JsonDataEditor jsonDataEditor = (JsonDataEditor) fileEditor;

        DBJsonView jsonView = (DBJsonView) editableObjectFile.getObject();
        if (!jsonDataEditor.isLoaded()) return null;

        String dataLoadError = jsonDataEditor.getDataLoadError();
        if (Strings.isNotEmpty(dataLoadError)) {
            return new JsonDataEditorLoadErrorNotificationPanel(jsonView, fileEditor, dataLoadError);
        }

        if (editableObjectFile.getEnvironmentType().isReadonlyData()) {
            return new JsonDataEditorReadonlyNotificationPanel(jsonView, fileEditor);
        }

        return null;
    }
}
