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

package com.dbn.ml.action;

import com.dbn.common.Priority;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMLInterface;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.ui.MLPredictDialog;
import com.dbn.object.DBAIModel;
import com.dbn.object.action.AnObjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Action to run ad-hoc predictions on an existing ML model from the database browser.
 * Queries the model's input attributes and mining function from Oracle, then opens
 * the prediction dialog.
 *
 * @author ayoub allali
 */
public class AIModelPredictAction extends AnObjectAction<DBAIModel> {

    public AIModelPredictAction(@NotNull DBAIModel model) {
        super(model);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBAIModel model) {
        ConnectionHandler connection = model.getConnection();
        String modelName = model.getName();

        try {
            DatabaseInterfaceInvoker.execute(Priority.HIGH,
                    "Loading Model Metadata",
                    "Querying attributes for model " + modelName,
                    project,
                    connection.getConnectionId(),
                    conn -> {
                        DatabaseMLInterface ml = connection.getInterfaces().getMLInterface();

                        String function = ml.getModelFunction(conn, modelName);
                        if (function == null) {
                            Dispatch.run(() -> Messages.showWarningDialog(project,
                                    "Model '" + modelName + "' not found in USER_MINING_MODELS.",
                                    "Model Not Found"));
                            return;
                        }

                        MLTaskType taskType = "CLASSIFICATION".equalsIgnoreCase(function)
                                ? MLTaskType.CLASSIFICATION
                                : MLTaskType.REGRESSION;

                        List<String> features = new ArrayList<>();
                        try (ResultSet rs = ml.getModelInputAttributes(conn, modelName)) {
                            while (rs.next()) {
                                features.add(rs.getString("ATTRIBUTE_NAME"));
                            }
                        }

                        if (features.isEmpty()) {
                            Dispatch.run(() -> Messages.showWarningDialog(project,
                                    "No input attributes found for model '" + modelName + "'.",
                                    "Cannot Predict"));
                            return;
                        }

                        MLTaskType finalTaskType = taskType;
                        List<String> finalFeatures = features;
                        Dispatch.run(() -> {
                            MLPredictDialog dialog = new MLPredictDialog(connection, modelName, finalTaskType, finalFeatures);
                            dialog.show();
                        });
                    });
        } catch (Exception ex) {
            Dispatch.run(() -> Messages.showErrorDialog(project,
                    "Failed to load model metadata: " + ex.getMessage(),
                    "Predict Error"));
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBAIModel target) {
        presentation.setText("Predict...");
        presentation.setIcon(Icons.ACTION_EXECUTE);
        presentation.setVisible(target != null);
        presentation.setEnabled(target != null);
    }
}
