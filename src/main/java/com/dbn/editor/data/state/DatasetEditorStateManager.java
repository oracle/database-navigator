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

package com.dbn.editor.data.state;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.util.Dialogs;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.state.column.ui.DatasetColumnSetupDialog;
import com.dbn.editor.data.state.sorting.ui.DatasetEditorSortingDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;

@State(
    name = DatasetEditorStateManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatasetEditorStateManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DatasetEditorStateManager";

    private DatasetEditorStateManager(Project project) {
        super(project, COMPONENT_NAME);
    }


    public static DatasetEditorStateManager getInstance(@NotNull Project project) {
        return projectService(project, DatasetEditorStateManager.class);
    }

    public void openSortingDialog(@NotNull DatasetEditor datasetEditor) {
        Dialogs.show(() -> new DatasetEditorSortingDialog(datasetEditor));
    }

    public void openColumnSetupDialog(@NotNull DatasetEditor datasetEditor) {
        Dialogs.show(() -> new DatasetColumnSetupDialog(datasetEditor));
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }

}
