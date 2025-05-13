/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.editor.json.action;

import com.dbn.common.icon.Icons;
import com.dbn.editor.json.JsonDataEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonDataContentEditorToggleAction extends AbstractJsonDataEditorAction {

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable JsonDataEditor editor) {
        if (editor == null) {
            presentation.setEnabled(false);
            presentation.setIcon(Icons.ACTION_LAYOUT_DATA_CONTENT);
            presentation.setText("Show / Hide Content Editor");
        } else {
            boolean visible = editor.isContentEditorVisible();
            presentation.setText(visible ? "Hide Content Editor" : "Show Content Editor");
            presentation.setIcon(visible ? Icons.ACTION_LAYOUT_DATA : Icons.ACTION_LAYOUT_DATA_CONTENT);
            boolean enabled = !editor.isInserting();
            presentation.setEnabled(enabled);
        }
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull JsonDataEditor editor) {
        editor.toggleContentEditorVisible();
    }
}