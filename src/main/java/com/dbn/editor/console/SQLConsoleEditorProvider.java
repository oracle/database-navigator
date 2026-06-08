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

package com.dbn.editor.console;

import com.dbn.common.editor.BasicTextEditorProvider;
import com.dbn.editor.EditorProviderId;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBSearchConsoleVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;


public class SQLConsoleEditorProvider extends BasicTextEditorProvider implements DumbAware{

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBSearchConsoleVirtualFile) return false;
        return virtualFile instanceof DBConsoleVirtualFile;
    }

    @Override
    @NotNull
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        SQLConsoleEditorState editorState = new SQLConsoleEditorState();
        editorState.readState(sourceElement, project, virtualFile);
        return editorState;
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (state instanceof SQLConsoleEditorState editorState) {
            editorState.writeState(targetElement, project);
        }
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        DBConsoleVirtualFile consoleFile = (DBConsoleVirtualFile) file;
        SQLConsoleEditor editor = new SQLConsoleEditor(project, consoleFile, txt("app.codeEditor.title.SqlConsole"), getEditorProviderId());

        Document document = editor.getEditor().getDocument();
        document.addDocumentListener(consoleFile, editor);

        return editor;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        editor.dispose();
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    @NotNull
    @Override
    public EditorProviderId getEditorProviderId() {
        return EditorProviderId.CONSOLE;
    }

    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.SQLConsoleEditorProvider";
    }

}
