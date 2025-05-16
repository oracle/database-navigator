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

import com.dbn.common.environment.EnvironmentManager;
import com.dbn.common.icon.Icons;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.editor.DBContentType.DATA;
import static com.dbn.nls.NlsResources.txt;

public class RecordInsertAction extends AbstractDataEditorAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatasetEditor datasetEditor) {
        datasetEditor.insertRecord();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatasetEditor datasetEditor) {
        boolean enabled = isEnabled(datasetEditor);
        boolean visible = isVisible(datasetEditor);

        presentation.setText(txt("app.dataEditor.action.InsertRecord"));
        presentation.setIcon(Icons.DATA_EDITOR_INSERT_RECORD);
        presentation.setVisible(visible);
        presentation.setEnabled(enabled);
    }

    private boolean isVisible(DatasetEditor datasetEditor) {
        if (isNotValid(datasetEditor)) return false;
        if (datasetEditor.isReadonlyData()) return false;
        DBDataset dataset = datasetEditor.getDataset();

        if (dataset.getEnvironmentType().isReadonlyData()) {
            return EnvironmentManager.isTransientlyEditable(dataset, DATA);
        }

        return true;
    }

    private boolean isEnabled(DatasetEditor datasetEditor) {
        if (isNotValid(datasetEditor)) return false;

        if (datasetEditor.isDirty()) return false;
        if (datasetEditor.isLoading()) return false;
        if (datasetEditor.isInserting()) return false;
        if (datasetEditor.isReadonly()) return false;
        if (!datasetEditor.isConnected()) return false;

        return true;
    }
}