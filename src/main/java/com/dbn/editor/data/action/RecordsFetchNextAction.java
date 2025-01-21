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

import com.dbn.common.dispose.Checks;
import com.dbn.common.icon.Icons;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.options.DataEditorSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class RecordsFetchNextAction extends AbstractDataEditorAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatasetEditor datasetEditor) {
        DataEditorSettings settings = DataEditorSettings.getInstance(datasetEditor.getProject());
        datasetEditor.fetchNextRecords(settings.getGeneralSettings().getFetchBlockSize().value());
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatasetEditor datasetEditor) {
        DataEditorSettings settings = DataEditorSettings.getInstance(project);
        Integer count = settings.getGeneralSettings().getFetchBlockSize().value();
        presentation.setText(txt("app.dataEditor.action.FetchNextRecords", count));
        presentation.setIcon(Icons.DATA_EDITOR_FETCH_NEXT_RECORDS);

        boolean enabled =
                Checks.isValid(datasetEditor) &&
                        datasetEditor.isLoaded() &&
                        datasetEditor.getConnection().isConnected() &&
                        !datasetEditor.isInserting() &&
                        !datasetEditor.isLoading() &&
                        !datasetEditor.isDirty() &&
                        !datasetEditor.getEditorTable().getModel().isResultSetExhausted();
        presentation.setEnabled(enabled);
    }
}