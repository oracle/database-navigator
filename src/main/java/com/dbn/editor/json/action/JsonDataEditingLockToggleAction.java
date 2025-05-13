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

import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.icon.Icons;
import com.dbn.editor.json.JsonDataEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class JsonDataEditingLockToggleAction extends AbstractJsonDataEditorAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull JsonDataEditor editor) {
        editor.toggleEditingLock();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable JsonDataEditor jsonDataEditor) {
        if (jsonDataEditor == null) {
            presentation.setEnabled(false);
            presentation.setIcon(Icons.DATA_EDITOR_LOCKED);
            presentation.setText(txt("app.dataEditor.action.LockUnlockEditing"));
            return;
        }

        boolean visible = isVisible(jsonDataEditor);
        boolean enabled = isEnabled(jsonDataEditor);
        boolean locked = jsonDataEditor.isEditingLocked();

        presentation.setText(locked ? txt("app.dataEditor.action.UnlockEditing") : txt("app.dataEditor.action.LockEditing"));
        presentation.setIcon(locked ? Icons.DATA_EDITOR_LOCKED : Icons.DATA_EDITOR_UNLOCKED);
        presentation.setVisible(visible);
        presentation.setEnabled(enabled);
    }

    private boolean isVisible(JsonDataEditor jsonDataEditor) {
        if (jsonDataEditor.isReadonlyData()) return false;

        EnvironmentType environmentType = jsonDataEditor.getDataset().getEnvironmentType();
        if (environmentType.isReadonlyData()) return false;

        return true;
    }

    private boolean isEnabled(JsonDataEditor jsonDataEditor) {
        if (jsonDataEditor.isInserting()) return false;
        if (jsonDataEditor.getTableModel().isModified()) return false;

        return true;
    }

}