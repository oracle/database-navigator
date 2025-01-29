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

package com.dbn.editor.data.filter.action;

import com.dbn.common.icon.Icons;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.ui.DatasetFilterList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DeleteFilterAction extends AbstractFilterListAction {

    public DeleteFilterAction(DatasetFilterList filterList) {
        super(filterList);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<DatasetFilter> selectedFilters = getFilterList().getSelectedValuesList();
        for (DatasetFilter filter : selectedFilters) {
            getFilterGroup().deleteFilter(filter);
            if (getFilterList().getModel().getSize() > 0) {
                getFilterList().setSelectedIndex(0);
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.dataEditor.action.DeleteFilter"));
        presentation.setIcon(Icons.ACTION_REMOVE);
    }
}
