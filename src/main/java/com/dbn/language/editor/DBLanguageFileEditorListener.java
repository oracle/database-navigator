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

package com.dbn.language.editor;

import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Editors;
import com.dbn.language.editor.ui.DBLanguageFileEditorToolbarForm;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.action.UserDataKeys.EDITOR_TOOLBAR_INSTALLED;
import static com.dbn.common.action.UserDataKeys.isUserData;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Files.isDbLanguageFile;
import static com.dbn.common.util.Files.isLightVirtualFile;

public class DBLanguageFileEditorListener extends DBNFileEditorManagerListener {
    @Override
    public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (!isInScope(file)) return;

        FileEditor[] fileEditors = source.getEditors(file);
        for (FileEditor fileEditor : fileEditors) {
            ensureToolbar(fileEditor, source, file);
        }
    }

    @Override
    public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile file = event.getNewFile();
        if (!isInScope(file)) return;

        FileEditor fileEditor = event.getNewEditor();
        FileEditorManager fileEditorManager = event.getManager();
        ensureToolbar(fileEditor, fileEditorManager, file);
    }

    private static boolean isInScope(VirtualFile file) {
        if (isNotValid(file)) return false;
        if (!isDbLanguageFile(file)) return false;
        return file.isInLocalFileSystem() || isLightVirtualFile(file);
    }

    private static void ensureToolbar(FileEditor fileEditor, FileEditorManager source, VirtualFile file) {
        if (isNotValid(fileEditor)) return;
        if (isUserData(fileEditor, EDITOR_TOOLBAR_INSTALLED)) return;

        DBLanguageFileEditorToolbarForm toolbarForm = new DBLanguageFileEditorToolbarForm(fileEditor, source.getProject(), file);
        Editors.addEditorToolbar(fileEditor, toolbarForm);
        fileEditor.putUserData(EDITOR_TOOLBAR_INSTALLED, true);
    }
}
