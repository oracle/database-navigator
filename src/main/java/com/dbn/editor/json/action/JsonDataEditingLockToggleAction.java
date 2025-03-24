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

import com.dbn.common.action.Lookups;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.icon.Icons;
import com.dbn.editor.json.JsonDataEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class JsonDataEditingLockToggleAction extends ToggleAction implements DumbAware {

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        JsonDataEditor editor = getJsonDataEditor(e);
        return editor != null && editor.isReadonly();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
        JsonDataEditor editor = getJsonDataEditor(e);
        if (editor != null) editor.setReadonly(selected);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        JsonDataEditor editor = getJsonDataEditor(e);
        Presentation presentation = e.getPresentation();
        Project project = e.getProject();
        if (project == null || editor == null) {
            presentation.setEnabled(false);
            presentation.setIcon(Icons.DATA_EDITOR_LOCKED);
            presentation.setText(txt("app.dataEditor.action.LockUnlockEditing"));
        } else {
            boolean isEnvironmentReadonlyData = editor.getJsonView().getEnvironmentType().isReadonlyData();
            presentation.setVisible(!editor.isReadonlyData() && !isEnvironmentReadonlyData);
            boolean selected = isSelected(e);
            presentation.setText(selected ? txt("app.dataEditor.action.UnlockEditing") : txt("app.dataEditor.action.LockEditing"));
            presentation.setIcon(selected ? Icons.DATA_EDITOR_LOCKED : Icons.DATA_EDITOR_UNLOCKED);
            boolean enabled = !editor.isInserting();
            presentation.setEnabled(enabled);
        }

    }

    private static JsonDataEditor getJsonDataEditor(AnActionEvent e) {
        FileEditor fileEditor = Lookups.getFileEditor(e);
        return fileEditor instanceof JsonDataEditor ? (JsonDataEditor) fileEditor : null;
    }
}