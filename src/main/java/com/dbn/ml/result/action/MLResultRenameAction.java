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
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMLInterface;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.backend.model.MLModelHandle;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.MLExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JOptionPane;

/**
 * Action to rename ML model.
 * For DBMS backend: renames model in database using DBMS_DATA_MINING.RENAME_MODEL.
 * For Tribuo backend: only renames the UI tab (model is in-memory).
 *
 * @author ayoub allali
 */
public class MLResultRenameAction extends AbstractMLExecutionResultAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MLExecutionResult executionResult) {
        MLResult result = executionResult.getMlResult();
        MLModelHandle modelHandle = result.getModelHandle();

        String currentModelName = getCurrentModelName(result);
        String defaultName = buildDefaultName(result, currentModelName);

        String newName = JOptionPane.showInputDialog(
                null,
                "Enter a new name for the model:",
                defaultName
        );

        if (newName == null || newName.trim().isEmpty()) return;
        newName = newName.trim().toUpperCase();

        if (newName.equals(currentModelName)) return; // No change

        // For DBMS backend, rename in database
        if (result.getBackendType() == MLBackendType.DBMS_DATA_MINING && modelHandle instanceof DBMSModelHandle dbmsHandle) {
            renameInDatabase(project, executionResult, dbmsHandle, currentModelName, newName);
        } else {
            // For Tribuo (in-memory), just rename the UI tab
            executionResult.setName(newName, true);
        }
    }

    private void renameInDatabase(Project project, MLExecutionResult executionResult,
                                   DBMSModelHandle modelHandle, String oldName, String newName) {
        Background.run(() -> {
            try {
                ConnectionHandler connection = modelHandle.getConnection();
                DatabaseMLInterface mlInterface = connection.getInterfaces().getMLInterface();

                try (DBNConnection conn = connection.getMainConnection()) {
                    mlInterface.renameModel(conn, oldName, newName);

                    // Update the handle with new name
                    modelHandle.setModelName(newName);

                    Dispatch.run(() -> {
                        executionResult.setName(newName, true);
                        Messages.showInfoDialog(project,
                                "Model renamed from '" + oldName + "' to '" + newName + "'",
                                "Rename Complete");
                    });
                }
            } catch (Exception ex) {
                Dispatch.run(() -> Messages.showErrorDialog(project,
                        "Failed to rename model: " + ex.getMessage(),
                        "Rename Failed"));
            }
        });
    }

    private String getCurrentModelName(MLResult result) {
        MLModelHandle handle = result.getModelHandle();
        if (handle instanceof DBMSModelHandle dbmsHandle) {
            return dbmsHandle.getModelName();
        }
        return result.getAlgorithmName();
    }

    private String buildDefaultName(MLResult result, String currentModelName) {
        // For rename, show the actual current model name
        if (currentModelName != null && !currentModelName.isEmpty()) {
            return currentModelName;
        }
        // Fallback to source-based name
        String sourceName = result.getSourceName();
        if (sourceName != null && !sourceName.isEmpty()) {
            return sourceName.toUpperCase() + "_MODEL";
        }
        return "ML_MODEL";
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MLExecutionResult target) {
        presentation.setText("Rename Model");
        presentation.setIcon(Icons.ACTION_EDIT);
        presentation.setVisible(target != null);
        presentation.setEnabled(target != null);
    }
}
