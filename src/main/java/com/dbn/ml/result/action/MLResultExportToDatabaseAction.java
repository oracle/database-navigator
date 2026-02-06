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
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.MLExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tribuo.Model;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;

/**
 * Action to save ML model to database.
 *
 * @author ayoub allali
 */
public class MLResultExportToDatabaseAction extends AbstractMLExecutionResultAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MLExecutionResult executionResult) {
        MLResult result = executionResult.getMlResult();
        Model<?> model = result.getTribuoModel();
        if (model == null) return;

        String defaultName = buildDefaultModelName(result);
        String modelName = JOptionPane.showInputDialog(
                null,
                "Enter a name for the model:",
                defaultName
        );

        if (modelName == null || modelName.trim().isEmpty()) return;

        Background.run(() -> {
            try {
                ConnectionHandler connection = result.getConnection();
                DatabaseInterfaces interfaces = connection.getInterfaces();
                DatabaseVectorInterface vectorInterface = interfaces.getVectorInterface();
                String ownerName = connection.getUserName();

                try (DBNConnection conn = connection.getMainConnection()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                        oos.writeObject(model);
                    }
                    byte[] modelBytes = baos.toByteArray();

                    Blob blob = conn.createBlob();
                    blob.setBytes(1, modelBytes);

                    vectorInterface.createModelFromFile(conn, ownerName, modelName, blob);

                    Dispatch.run(() -> Messages.showInfoDialog(
                            project,
                            "Model saved to database as: " + modelName,
                            "Save Complete"
                    ));
                }
            } catch (Exception ex) {
                Dispatch.run(() -> Messages.showErrorDialog(
                        project,
                        "Failed to save model: " + ex.getMessage(),
                        "Save Failed"
                ));
            }
        });
    }

    /**
     * Builds default model name from source name.
     * For database tables: TABLE_NAME_MODEL
     * For CSV files: FILE_NAME_MODEL
     */
    private String buildDefaultModelName(MLResult result) {
        String sourceName = result.getSourceName();
        if (sourceName == null || sourceName.isEmpty()) {
            return "ML_MODEL";
        }
        return sourceName.toUpperCase() + "_MODEL";
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MLExecutionResult target) {
        presentation.setText("Save to Database");
        presentation.setIcon(Icons.DBO_TABLE);

        boolean visible = target != null && target.getMlResult().getBackendType() == MLBackendType.TRIBUO;
        presentation.setVisible(visible);
        presentation.setEnabled(visible);
    }
}
