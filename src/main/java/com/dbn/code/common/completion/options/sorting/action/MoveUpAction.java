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

package com.dbn.code.common.completion.options.sorting.action;

import com.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.ListUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;

import static com.dbn.nls.NlsResources.txt;

public class MoveUpAction extends BasicAction {
    private final CodeCompletionSortingSettings settings;
    private final JList list;

    public MoveUpAction(JList list, CodeCompletionSortingSettings settings)  {
        super(txt("app.data.action.MoveUp"), null, Icons.ACTION_MOVE_UP);
        this.list = list;
        this.settings = settings;
    }

    @Override
    public void update(AnActionEvent e) {
        int[] indices = list.getSelectedIndices();
        boolean enabled =
                list.isEnabled() &&
                indices.length > 0 &&
                indices[0] > 0;
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ListUtil.moveSelectedItemsUp(list);
        settings.setModified(true);
    }
}
