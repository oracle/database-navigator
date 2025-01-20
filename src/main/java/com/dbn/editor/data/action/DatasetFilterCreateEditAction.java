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

import com.dbn.common.icon.Icons;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.filter.DatasetFilterType;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;

public class DatasetFilterCreateEditAction extends AbstractDataEditorAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatasetEditor datasetEditor) {
        DBDataset dataset = datasetEditor.getDataset();
        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
        DatasetFilter activeFilter = filterManager.getActiveFilter(dataset);
        if (activeFilter == null || activeFilter.getFilterType() == DatasetFilterType.NONE) {
            DataEditorSettings settings = DataEditorSettings.getInstance(dataset.getProject());
            DatasetFilterType filterType = settings.getFilterSettings().getDefaultFilterType();
            if (filterType == null || filterType == DatasetFilterType.NONE) {
                filterType = DatasetFilterType.BASIC;
            }


            filterManager.openFiltersDialog(dataset, false, true, filterType, null);
        }
        else {
            filterManager.openFiltersDialog(dataset, false, false,DatasetFilterType.NONE, null);
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatasetEditor datasetEditor) {
        if (isValid(datasetEditor) && datasetEditor.getConnection().isConnected()) {
            DBDataset dataset = datasetEditor.getDataset();
            boolean enabled = !datasetEditor.isInserting() && !datasetEditor.isLoading();

            presentation.setEnabled(enabled);

            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
            DatasetFilter activeFilter = filterManager.getActiveFilter(dataset);
            if (activeFilter == null || activeFilter.getFilterType() == DatasetFilterType.NONE) {
                presentation.setText(txt("app.dataEditor.action.CreateFilter"));
                presentation.setIcon(Icons.DATASET_FILTER_NEW);
            } else {
                presentation.setText(txt("app.dataEditor.action.EditFilter"));
                presentation.setIcon(Icons.DATASET_FILTER_EDIT);
            }
        } else {
            presentation.setEnabled(false);
        }
    }
}
