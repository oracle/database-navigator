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

package com.dbn.project;

import com.dbn.assistant.AssistantInitializationManager;
import com.dbn.common.component.EagerService;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.connection.session.DatabaseSessionManager;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.editor.DatabaseEditorStateManager;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.execution.compiler.DatabaseCompilerManager;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.object.common.loader.DatabaseLoaderManager;
import com.dbn.options.ProjectSettingsProvider;
import com.dbn.vfs.DBVirtualFile;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.component.Components.projectService;

/**
 * TODO SERVICES
 * TODO find another way to define "silent" dependencies
 */
@Getter
public class ProjectComponentsInitializer extends ProjectComponentBase implements /*StartupActivity, */DumbAware, EagerService {
    public static final String COMPONENT_NAME = "DBNavigator.Project.WorkspaceInitializer";
    private boolean initialized;


    public ProjectComponentsInitializer(Project project) {
        super(project, COMPONENT_NAME);
        ProjectSettingsProvider.init(project);
        ConnectionBundleSettings.init(project);
        ProjectEvents.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, componentInitializer());
    }

    @NotNull
    public static ProjectComponentsInitializer getInstance(@NotNull Project project) {
        return projectService(project, ProjectComponentsInitializer.class);
    }

    private FileEditorManagerListener.Before componentInitializer() {
        return new FileEditorManagerListener.Before() {
            @Override
            public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                Project project = source.getProject();

                ProjectComponentsInitializer initializer = getInstance(project);
                if (initializer.shouldInitialize(file)) initializer.initializeComponents();
            }
        };
    }

    private boolean shouldInitialize(VirtualFile file) {
        if (initialized) return false;
        if (file instanceof DBVirtualFile) return true;
        if (file.getFileType() instanceof DBLanguageFileType) return true;
        return false;
    }

    public void initializeComponents() {
        Project project = getProject();
        DatabaseConsoleManager.getInstance(project);
        DatabaseEditorStateManager.getInstance(project);
        SourceCodeManager.getInstance(project);
        DatasetEditorManager.getInstance(project);
        DatabaseCompilerManager.getInstance(project);
        DDLFileAttachmentManager.getInstance(project);
        DatabaseLoaderManager.getInstance(project);
        DatabaseSessionManager.getInstance(project);
        DatabaseFileEditorManager.getInstance(project);
        AssistantInitializationManager.getInstance(project);
        initialized = true;
    }
}
