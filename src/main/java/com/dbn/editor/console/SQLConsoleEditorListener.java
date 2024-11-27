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

import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Editors;
import com.dbn.editor.console.ui.SQLConsoleEditorToolbarForm;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Files.isDbConsoleFile;

public class SQLConsoleEditorListener extends DBNFileEditorManagerListener {
  @Override
  public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    if (isNotValid(file)) return;
    if (!isDbConsoleFile(file)) return;

    SQLConsoleEditor fileEditor = (SQLConsoleEditor) source.getSelectedEditor(file);
    if (isNotValid(fileEditor)) return;

    Project project = source.getProject();
    SQLConsoleEditorToolbarForm toolbarForm = new SQLConsoleEditorToolbarForm(project, fileEditor);
    Editors.addEditorToolbar(fileEditor, toolbarForm);
//
//    Document document = FileDocumentManager.getInstance().getDocument(file);
//    if (document != null) {
//      document.addDocumentListener(new EditorAIQueryListener(source.getProject()));
//    }
  }
}
