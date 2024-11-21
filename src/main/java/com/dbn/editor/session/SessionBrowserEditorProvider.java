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

package com.dbn.editor.session;

import com.dbn.common.dispose.Disposer;
import com.dbn.editor.EditorProviderId;
import com.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SessionBrowserEditorProvider implements FileEditorProvider, NamedComponent, DumbAware {
    /*********************************************************
     *                  FileEditorProvider                   *
     *********************************************************/

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile instanceof DBSessionBrowserVirtualFile;
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        DBSessionBrowserVirtualFile sessionBrowserFile = (DBSessionBrowserVirtualFile) file;
        return new SessionBrowser(sessionBrowserFile);
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        Disposer.dispose(editor);
    }

    @Override
    @NotNull
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBSessionBrowserVirtualFile) {
            SessionBrowserState sessionBrowserState = new SessionBrowserState();
            sessionBrowserState.readState(sourceElement);
            return sessionBrowserState;
        }
        return new SessionBrowserState();
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (state instanceof SessionBrowserState) {
            SessionBrowserState editorState = (SessionBrowserState) state;
            editorState.writeState(targetElement);
        }
    }

    @Override
    @NotNull
    @NonNls
    public String getEditorTypeId() {
        return EditorProviderId.SESSION_BROWSER.getId();
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/
    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.SessionBrowserProvider";
    }
}

