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

package com.dbn.editor.code.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.util.Editors;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@BackgroundUpdate
public abstract class AbstractCodeEditorAction extends ProjectAction {

    @Nullable
    protected static Editor getEditor(AnActionEvent e) {
        return Lookups.getEditor(e);
    }

    @Override
    protected final void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        SourceCodeEditor fileEditor = getFileEditor(e);
        DBSourceCodeVirtualFile sourcecodeFile = getSourcecodeFile(e);
        if (fileEditor != null && sourcecodeFile != null) {
            actionPerformed(e, project, fileEditor, sourcecodeFile);
        }
    }

    @Override
    protected final void update(@NotNull AnActionEvent e, @NotNull Project project) {
        update(e, project, getFileEditor(e), getSourcecodeFile(e));
    }

    @Nullable
    protected static SourceCodeEditor getFileEditor(AnActionEvent e) {
        Editor editor = getEditor(e);
        FileEditor fileEditor = Editors.getFileEditor(editor);
        return fileEditor instanceof SourceCodeEditor ? (SourceCodeEditor) fileEditor : null;
    }

    @Nullable
    protected static DBSourceCodeVirtualFile getSourcecodeFile(AnActionEvent e) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (virtualFile instanceof DBSourceCodeVirtualFile) return (DBSourceCodeVirtualFile) virtualFile;

        SourceCodeEditor fileEditor = getFileEditor(e);
        if (fileEditor == null) return null;

        return fileEditor.getVirtualFile();
    }

    protected abstract void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull SourceCodeEditor fileEditor,
            @NotNull DBSourceCodeVirtualFile sourceCodeFile);

    protected abstract void update(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @Nullable SourceCodeEditor fileEditor,
            @Nullable DBSourceCodeVirtualFile sourceCodeFile);
}
