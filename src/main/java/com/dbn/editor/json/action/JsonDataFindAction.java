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

package com.dbn.editor.json.action;

import com.dbn.common.icon.Icons;
import com.dbn.editor.json.JsonDataEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class JsonDataFindAction extends AbstractJsonDataEditorAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull JsonDataEditor jsonDataEditor) {
        jsonDataEditor.showSearchHeader();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable JsonDataEditor jsonDataEditor) {
        presentation.setText(txt("app.dataEditor.action.FindData"));
        presentation.setIcon(Icons.ACTION_FIND);

        if (jsonDataEditor == null) {
            presentation.setEnabled(false);
        } else {
            presentation.setEnabled(true);
            if (jsonDataEditor.isInserting() || jsonDataEditor.isLoading()) {
                presentation.setEnabled(false);
            }
        }

    }
}
