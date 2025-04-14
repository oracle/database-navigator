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
import com.dbn.common.util.Messages;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;

public class DataImportAction extends AbstractDataEditorAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatasetEditor datasetEditor) {
        Messages.showInfoDialog(project,
                txt("msg.dataEditor.title.DataImportNotSupported"),
                txt("msg.dataEditor.info.DataImportNotSupported"));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatasetEditor datasetEditor) {
        presentation.setText(txt("app.dataEditor.action.ImportData"));
        presentation.setIcon(Icons.DATA_IMPORT);

        if (isValid(datasetEditor)) {
            DBDataset dataset = datasetEditor.getDataset();
            boolean environmentReadonlyData = dataset.getEnvironmentType().isReadonlyData();
            boolean visible = !environmentReadonlyData && !datasetEditor.isReadonlyData();

            presentation.setVisible(visible);
/*
            boolean enabled =
                    datasetEditor.getConnectionHandler().isConnected() &&
                    !datasetEditor.isReadonly() &&
                    !datasetEditor.isInserting();
*/
            boolean enabled = false;
            presentation.setEnabled(enabled);
        } else {
            presentation.setEnabled(false);
        }
    }
}