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

import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Files;
import com.dbn.editor.code.ui.SourceCodeEditorToolbarForm;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.action.UserDataKeys.SOURCE_CODE_EDITOR_TOOLBAR_INSTALLED;
import static com.dbn.common.action.UserDataKeys.isUserData;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.file.util.VirtualFiles.isLocalFileSystem;
import static com.dbn.common.util.Files.isDbConsoleFile;

public class SourceCodeEditorListener extends DBNFileEditorManagerListener {
    @Override
    public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (isNotValid(file)) return;
        if (isDbConsoleFile(file)) return;
        if (isLocalFileSystem(file)) return;
        if (!Files.isDbEditableObjectFile(file)) return;

        FileEditor[] fileEditors = source.getEditors(file);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof SourceCodeEditor sourceCodeEditor) {
                ensureToolbar(sourceCodeEditor);
            }
        }
    }

    @Override
    public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
        FileEditor editor = event.getNewEditor();
        if (editor instanceof SourceCodeEditor sourceCodeEditor) {
            ensureToolbar(sourceCodeEditor);
        }
    }

    private static void ensureToolbar(@NotNull SourceCodeEditor sourceCodeEditor) {
        if (isUserData(sourceCodeEditor, SOURCE_CODE_EDITOR_TOOLBAR_INSTALLED)) return;

        SourceCodeEditorToolbarForm actionsPanel = new SourceCodeEditorToolbarForm(sourceCodeEditor);
        Editors.addEditorToolbar(sourceCodeEditor, actionsPanel);
        sourceCodeEditor.putUserData(SOURCE_CODE_EDITOR_TOOLBAR_INSTALLED, true);
    }

}
