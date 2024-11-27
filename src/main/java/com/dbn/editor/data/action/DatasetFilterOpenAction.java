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

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.filter.DatasetFilterType;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class DatasetFilterOpenAction extends BasicAction {
    private DatasetEditor datasetEditor;
    DatasetFilterOpenAction(DatasetEditor datasetEditor) {
        super("Manage Filters...", null, Icons.ACTION_EDIT);
        this.datasetEditor = datasetEditor;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (datasetEditor != null) {
            DBDataset dataset = datasetEditor.getDataset();
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
            filterManager.openFiltersDialog(dataset, false, false, DatasetFilterType.NONE, null);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        boolean enabled = datasetEditor != null && !datasetEditor.isInserting();
        e.getPresentation().setEnabled(enabled);

    }
}
