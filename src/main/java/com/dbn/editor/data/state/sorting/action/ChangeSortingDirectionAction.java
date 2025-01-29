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

package com.dbn.editor.data.state.sorting.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.sorting.SortingInstruction;
import com.dbn.editor.data.state.sorting.ui.DatasetSortingColumnForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

public class ChangeSortingDirectionAction extends BasicAction {
    private final DatasetSortingColumnForm form;

    public ChangeSortingDirectionAction(DatasetSortingColumnForm form) {
        this.form = form;
    }

    @Override
    public void update(AnActionEvent e) {
        SortingInstruction sortingInstruction = form.getSortingInstruction();
        SortDirection direction = sortingInstruction.getDirection();
        Icon icon =
            direction == SortDirection.ASCENDING ? Icons.DATA_SORTING_ASC :
            direction == SortDirection.DESCENDING ? Icons.DATA_SORTING_DESC : null;
        Presentation presentation = e.getPresentation();
        presentation.setIcon(icon);
        presentation.setText(txt("app.dataEditor.action.ChangeSortingDirection"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        form.getSortingInstruction().switchDirection();
    }

}
