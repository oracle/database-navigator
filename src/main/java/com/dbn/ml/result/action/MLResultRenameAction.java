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
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.MLExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JOptionPane;

import static com.dbn.nls.NlsResources.txt;

/**
 * Action to rename ML model in the database using DBMS_DATA_MINING.RENAME_MODEL.
 *
 * @author ayoub allali
 */
public class MLResultRenameAction extends AbstractMLExecutionResultAction {
    private static final @NonNls String DEFAULT_MODEL_NAME = "ML_MODEL";


    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MLExecutionResult executionResult) {
        MLResult result = executionResult.getMlResult();
        DBMSModelHandle modelHandle = result.getModelHandle();

        String currentModelName = getCurrentModelName(result);
        String defaultName = buildDefaultName(result, currentModelName);

        String newName = JOptionPane.showInputDialog(
                null,
                txt("msg.machineLearning.text.EnterModelName"),
                defaultName
        );

        if (newName == null || newName.trim().isEmpty()) return;
        newName = newName.trim().toUpperCase();

        if (newName.equals(currentModelName)) return; // No change

        renameInDatabase(project, executionResult, modelHandle, currentModelName, newName);
    }

    private void renameInDatabase(Project project, MLExecutionResult executionResult,
                                   DBMSModelHandle modelHandle, String oldName, String newName) {
        Background.run(() -> {
            try {
                ConnectionHandler connection = modelHandle.getConnection();
                DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();

                try (DBNConnection conn = connection.getMainConnection()) {
                    mlInterface.renameModel(conn, oldName, newName);

                    // Update the handle with new name
                    modelHandle.setModelName(newName);

                    Dispatch.run(() -> {
                        executionResult.setName(newName, true);
                        Messages.showInfoDialog(project,
                                txt("msg.machineLearning.title.RenameComplete"),
                                txt("msg.machineLearning.info.ModelRenamed", oldName, newName));
                    });
                }
            } catch (Exception ex) {
                Dispatch.run(() -> Messages.showErrorDialog(project,
                        txt("msg.machineLearning.title.RenameFailed"),
                        txt("msg.machineLearning.error.RenameModelFailed", ex)));
            }
        });
    }

    private String getCurrentModelName(MLResult result) {
        DBMSModelHandle handle = result.getModelHandle();
        return handle != null ? handle.getModelName() : result.getAlgorithmName();
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
        return DEFAULT_MODEL_NAME;
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MLExecutionResult target) {
        presentation.setText(txt("app.machineLearning.action.RenameModel"));
        presentation.setIcon(Icons.ACTION_EDIT);
        presentation.setVisible(target != null);
        presentation.setEnabled(target != null);
    }
}
