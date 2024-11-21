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

public class MoveFilterUpAction extends AbstractFilterListAction {

    public MoveFilterUpAction(DatasetFilterList filterList) {
        super(filterList);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DatasetFilterList filterList = getFilterList();
        DatasetFilter filter = (DatasetFilter) filterList.getSelectedValue();
        getFilterGroup().moveFilterUp(filter);
        filterList.setSelectedIndex(filterList.getSelectedIndex()-1);
    }

    @Override
    public void update(AnActionEvent e) {
        int[] index = getFilterList().getSelectedIndices();
        boolean enabled = index.length == 1 && index[0] > 0;

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        presentation.setText("Move Selection Up");
        presentation.setIcon(Icons.ACTION_MOVE_UP);
    }
}