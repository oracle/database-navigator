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

package com.dbn.editor.data.action;

import com.dbn.common.action.Lookups;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.icon.Icons;
import com.dbn.editor.data.DatasetEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class DataEditingLockToggleAction extends ToggleAction implements DumbAware {

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        DatasetEditor datasetEditor = getDatasetEditor(e);
        return datasetEditor != null && datasetEditor.isReadonly();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
        DatasetEditor datasetEditor = getDatasetEditor(e);
        if (datasetEditor != null) datasetEditor.setReadonly(selected);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        DatasetEditor datasetEditor = getDatasetEditor(e);
        Presentation presentation = e.getPresentation();
        Project project = e.getProject();
        if (project == null || datasetEditor == null) {
            presentation.setEnabled(false);
            presentation.setIcon(Icons.DATA_EDITOR_LOCKED);
            presentation.setText(txt("app.dataEditor.action.LockUnlockEditing"));
        } else {
            boolean isEnvironmentReadonlyData = datasetEditor.getDataset().getEnvironmentType().isReadonlyData();
            presentation.setVisible(!datasetEditor.isReadonlyData() && !isEnvironmentReadonlyData);
            boolean selected = isSelected(e);
            presentation.setText(selected ? txt("app.dataEditor.action.UnlockEditing") : txt("app.dataEditor.action.LockEditing"));
            presentation.setIcon(selected ? Icons.DATA_EDITOR_LOCKED : Icons.DATA_EDITOR_UNLOCKED);
            boolean enabled = !datasetEditor.isInserting();
            presentation.setEnabled(enabled);
        }

    }

    private static DatasetEditor getDatasetEditor(AnActionEvent e) {
        FileEditor fileEditor = Lookups.getFileEditor(e);
        return fileEditor instanceof DatasetEditor ? (DatasetEditor) fileEditor : null;
    }
}