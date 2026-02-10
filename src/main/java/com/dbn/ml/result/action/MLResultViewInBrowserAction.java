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

package com.dbn.ml.result.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.backend.model.MLModelHandle;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.MLExecutionResult;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action to view the ML model in the database browser.
 * Only available for DBMS models that are persisted in the database.
 *
 * @author ayoub allali
 */
public class MLResultViewInBrowserAction extends AbstractMLExecutionResultAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MLExecutionResult executionResult) {
        MLResult result = executionResult.getMlResult();
        MLModelHandle modelHandle = result.getModelHandle();

        if (!(modelHandle instanceof DBMSModelHandle dbmsHandle)) {
            Messages.showWarningDialog(project, "View in database is only available for DBMS models.", "Not Available");
            return;
        }

        String modelName = dbmsHandle.getModelName();
        ConnectionHandler connection = dbmsHandle.getConnection();

        // Get the user schema where the model is stored
        DBSchema schema = connection.getUserSchema();
        if (schema == null) {
            Messages.showWarningDialog(project, "Could not find user schema.", "Schema Not Found");
            return;
        }

        // Find the model by name
        DBAIModel aiModel = findAIModel(schema, modelName);
        if (aiModel == null) {
            Messages.showWarningDialog(project,
                    "Model '" + modelName + "' not found in database.\nIt may have been dropped or renamed.",
                    "Model Not Found");
            return;
        }

        // Navigate to the model in the browser
        aiModel.navigate(true);
    }

    @Nullable
    private DBAIModel findAIModel(DBSchema schema, String modelName) {
        for (DBAIModel model : schema.getAIModels()) {
            if (model.getName().equalsIgnoreCase(modelName)) {
                return model;
            }
        }
        return null;
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MLExecutionResult target) {
        presentation.setText("View in Database");
        presentation.setIcon(Icons.DBO_AI_MODEL);

        boolean visible = target != null && target.getMlResult().getBackendType() == MLBackendType.DBMS_DATA_MINING;
        presentation.setVisible(visible);
        presentation.setEnabled(visible);
    }
}
