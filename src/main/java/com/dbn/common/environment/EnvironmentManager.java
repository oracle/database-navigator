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

package com.dbn.common.environment;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.util.Editors;
import com.dbn.editor.DBContentType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.object.common.status.DBObjectStatus.EDITABLE;

@State(
    name = EnvironmentManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class EnvironmentManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.EnvironmentManager";

    private EnvironmentManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
    }

    public static EnvironmentManager getInstance(@NotNull Project project) {
        return projectService(project, EnvironmentManager.class);
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
                Editors.updateEditorPresentations(project, openFiles);
            }
        };
    }

    public boolean isReadonly(@NotNull DBContentVirtualFile contentFile) {
        return isReadonly(contentFile.getObject(), contentFile.getContentType());
    }

    public boolean isReadonly(@NotNull DBSchemaObject schemaObject, @NotNull DBContentType contentType) {
        EnvironmentType environmentType = schemaObject.getEnvironmentType();
        DBObjectStatusHolder objectStatus = schemaObject.getStatus();
        if (contentType == DBContentType.DATA) {
            return environmentType.isReadonlyData() && objectStatus.isNot(contentType, EDITABLE);
        } else {
            return environmentType.isReadonlyCode() && objectStatus.isNot(contentType, EDITABLE);
        }
    }

    public void enableEditing(@NotNull DBSchemaObject schemaObject, @NotNull DBContentType contentType) {
        schemaObject.getStatus().set(contentType, EDITABLE, true);
        DBContentVirtualFile contentFile = schemaObject.getEditableVirtualFile().getContentFile(contentType);
        if (isNotValid(contentFile)) return;

        Editors.setEditorsReadonly(contentFile, false);

        Project project = getProject();
        ProjectEvents.notify(project,
                EnvironmentManagerListener.TOPIC,
                (listener) -> listener.editModeChanged(project, contentFile));
    }

    public void disableEditing(@NotNull DBSchemaObject schemaObject, @NotNull DBContentType contentType) {
        schemaObject.getStatus().set(contentType, EDITABLE, false);

        DBContentVirtualFile contentFile = schemaObject.getEditableVirtualFile().getContentFile(contentType);
        if (isNotValid(contentFile)) return;

        boolean readonly = isReadonly(schemaObject, contentType);
        Editors.setEditorsReadonly(contentFile, readonly);

        Project project = getProject();
        ProjectEvents.notify(project,
                EnvironmentManagerListener.TOPIC,
                (listener) -> listener.editModeChanged(project, contentFile));

    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }
}
