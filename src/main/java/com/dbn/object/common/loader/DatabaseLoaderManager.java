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

package com.dbn.object.common.loader;

import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionLoadListener;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.component.Components.projectService;

public class DatabaseLoaderManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseLoaderManager";

    private DatabaseLoaderManager(Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, ConnectionLoadListener.TOPIC, connectionLoadListener(project));
    }

    public static DatabaseLoaderManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseLoaderManager.class);
    }

    @NotNull
    private ConnectionLoadListener connectionLoadListener(Project project) {
        return connection -> {
            List<Editor> editors = Dispatch.call(() -> getLinkedEditors(connection));
            Background.run(project, () -> Documents.refreshEditorAnnotations(editors));
        };
    }

    private List<Editor> getLinkedEditors(ConnectionHandler connection) {
        Project project = connection.getProject();
        List<Editor> editors = new ArrayList<>();

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
        for (VirtualFile openFile : openFiles) {
            checkDisposed();
            ConnectionHandler activeConnection = contextManager.getConnection(openFile);
            if (activeConnection != connection) continue;

            FileEditor[] fileEditors = fileEditorManager.getEditors(openFile);
            for (FileEditor fileEditor : fileEditors) {
                checkDisposed();
                Editor editor = Editors.getEditor(fileEditor);
                if (editor != null) editors.add(editor);
            }
        }

        return editors;
    }
}
