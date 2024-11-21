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

package com.dbn.editor.code;

import com.dbn.common.dispose.Checks;
import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Strings;
import com.dbn.editor.code.diff.SourceCodeDifManagerListener;
import com.dbn.editor.code.ui.SourceCodeEditorNotificationPanel;
import com.dbn.editor.code.ui.SourceCodeLoadErrorNotificationPanel;
import com.dbn.editor.code.ui.SourceCodeOutdatedNotificationPanel;
import com.dbn.editor.code.ui.SourceCodeReadonlyNotificationPanel;
import com.dbn.execution.script.ScriptExecutionListener;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SourceCodeEditorNotificationProvider extends EditorNotificationProvider<SourceCodeEditorNotificationPanel> {
    private static final Key<SourceCodeEditorNotificationPanel> KEY = Key.create("DBNavigator.SourceCodeEditorNotificationPanel");

    public SourceCodeEditorNotificationProvider() {
        ProjectEvents.subscribe(SourceCodeManagerListener.TOPIC, sourceCodeManagerListener());
        ProjectEvents.subscribe(SourceCodeDifManagerListener.TOPIC, sourceCodeDifManagerListener());
        ProjectEvents.subscribe(EnvironmentManagerListener.TOPIC, environmentManagerListener());
        ProjectEvents.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
        ProjectEvents.subscribe(ScriptExecutionListener.TOPIC, scriptExecutionListener());

    }

    @NotNull
    private ScriptExecutionListener scriptExecutionListener() {
        return (project, virtualFile) -> updateEditorNotification(project, null);
    }

    @NotNull
    private SourceCodeManagerListener sourceCodeManagerListener() {
        return new SourceCodeManagerListener() {
            @Override
            public void sourceCodeLoaded(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean initialLoad) {
                updateEditorNotification(sourceCodeFile.getProject(), sourceCodeFile);
            }

            @Override
            public void sourceCodeSaved(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor) {
                updateEditorNotification(sourceCodeFile.getProject(), sourceCodeFile);
            }
        };
    }


    @NotNull
    private SourceCodeDifManagerListener sourceCodeDifManagerListener() {
        return (sourceCodeFile, mergeAction) -> updateEditorNotification(sourceCodeFile.getProject(), sourceCodeFile);
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
                if (databaseContentFile instanceof DBSourceCodeVirtualFile) {
                    updateEditorNotification(project, databaseContentFile);
                }
            }
        };
    }

    @NotNull
    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
                VirtualFile virtualFile = event.getNewFile();
                if (virtualFile instanceof DBEditableObjectVirtualFile) {
                    DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;
                    for (DBSourceCodeVirtualFile sourceCodeFile : databaseFile.getSourceCodeFiles()) {
                        updateEditorNotification(sourceCodeFile.getProject(), sourceCodeFile);
                    }
                } else if (virtualFile instanceof DBSourceCodeVirtualFile) {
                    DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) virtualFile;
                    updateEditorNotification(sourceCodeFile.getProject(), sourceCodeFile);
                }
            }
        };
    }

    @NotNull
    @Override
    public Key<SourceCodeEditorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public SourceCodeEditorNotificationPanel createComponent(@NotNull VirtualFile virtualFile, @NotNull FileEditor fileEditor, @NotNull Project project) {
        if (!(virtualFile instanceof DBVirtualFile)) return null;
        if (!(fileEditor instanceof SourceCodeEditor) || !Checks.isValid(fileEditor)) return null;

        DBVirtualFile databaseFile = (DBVirtualFile) virtualFile;
        DBObject object = databaseFile.getObject();
        if (!(object instanceof DBSchemaObject)) return null;

        DBSchemaObject schemaObject = (DBSchemaObject) object;
        SourceCodeEditor sourceCodeEditor = (SourceCodeEditor) fileEditor;
        DBSourceCodeVirtualFile sourceCodeFile = sourceCodeEditor.getVirtualFile();
        String sourceLoadError = sourceCodeFile.getSourceLoadError();
        if (Strings.isNotEmpty(sourceLoadError)) {
            return new SourceCodeLoadErrorNotificationPanel(schemaObject, sourceLoadError);
        }

        if (sourceCodeFile.isChangedInDatabase(false)) {
            return new SourceCodeOutdatedNotificationPanel(sourceCodeFile, sourceCodeEditor);
        }

        if (sourceCodeFile.getEnvironmentType().isReadonlyCode()) {
            return new SourceCodeReadonlyNotificationPanel(schemaObject, sourceCodeEditor);
        }
        return null;
    }
}
