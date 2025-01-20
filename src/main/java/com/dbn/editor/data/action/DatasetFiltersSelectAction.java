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

import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.icon.Icons;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterGroup;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;

public class DatasetFiltersSelectAction extends ComboBoxAction {

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
        DatasetEditor datasetEditor = DatasetEditor.get(dataContext);

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        if (datasetEditor != null) {
            DBDataset dataset = datasetEditor.getDataset();
            DatasetFilterOpenAction datasetFilterOpenAction = new DatasetFilterOpenAction(datasetEditor);
            datasetFilterOpenAction.setInjectedContext(true);
            actionGroup.add(datasetFilterOpenAction);
            actionGroup.addSeparator();
            actionGroup.add(new DatasetFilterSelectAction(dataset, DatasetFilterManager.EMPTY_FILTER));
            actionGroup.addSeparator();

            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
            DatasetFilterGroup filterGroup = filterManager.getFilterGroup(dataset);
            for (DatasetFilter filter : filterGroup.getFilters()) {
                actionGroup.add(new DatasetFilterSelectAction(dataset, filter));
            }
        }
        return actionGroup;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DatasetEditor datasetEditor = DatasetEditor.get(e);

        Presentation presentation = e.getPresentation();
        boolean enabled =
                isValid(datasetEditor) &&
                !datasetEditor.isInserting() &&
                !datasetEditor.isLoading();

        if (isValid(datasetEditor)) {
            DBDataset dataset = datasetEditor.getDataset();

            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
            DatasetFilter activeFilter = filterManager.getActiveFilter(dataset);

            if (activeFilter == null) {
                presentation.setText(txt("app.dataEditor.action.NoFilter"));
                presentation.setIcon(Icons.DATASET_FILTER_EMPTY);
            } else {
                //e.getPresentation().setText(activeFilter.getName());
                presentation.setText(activeFilter.getName(), false);
                presentation.setIcon(activeFilter.getIcon());
            }
        }

        //if (!enabled) presentation.setIcon(null);
        presentation.setEnabled(enabled);
    }
}
