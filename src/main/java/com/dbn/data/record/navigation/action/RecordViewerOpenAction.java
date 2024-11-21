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

package com.dbn.data.record.navigation.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class RecordViewerOpenAction extends ProjectAction {
    private DatasetFilterInput filterInput;

    RecordViewerOpenAction(DatasetFilterInput filterInput) {
        this.filterInput = filterInput;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(project);
        datasetEditorManager.openRecordViewer(filterInput);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Record Viewer");
    }
}
