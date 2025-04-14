/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.editor.data.DataLoadInstructions;
import com.dbn.editor.json.JsonDataEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.editor.data.DataLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DataLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DataLoadInstruction.USE_CURRENT_FILTER;
import static com.dbn.nls.NlsResources.txt;

public class JsonDataReloadAction extends AbstractJsonDataEditorAction {

    private static final DataLoadInstructions LOAD_INSTRUCTIONS = new DataLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION);

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull JsonDataEditor jsonDataEditor) {
        jsonDataEditor.loadData(LOAD_INSTRUCTIONS);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable JsonDataEditor jsonDataEditor) {
        presentation.setText(txt("app.dataEditor.action.Reload"));
        presentation.setIcon(Icons.DATA_EDITOR_RELOAD_DATA);

        boolean enabled =
                isValid(jsonDataEditor) &&
                jsonDataEditor.isLoaded() &&
                !jsonDataEditor.isInserting() &&
                !jsonDataEditor.isLoading();
        presentation.setEnabled(enabled);

    }
}