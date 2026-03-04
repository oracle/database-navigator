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
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.MLExecutionResult;
import com.dbn.ml.ui.MLPredictDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Action for ad-hoc prediction using a trained DBMS model.
 * Shows a dialog to enter feature values and displays the prediction.
 *
 * @author ayoub allali
 */
public class MLResultPredictAction extends AbstractMLExecutionResultAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MLExecutionResult executionResult) {
        MLResult result = executionResult.getMlResult();

        List<String> featureColumns = result.getFeatureColumns();
        if (featureColumns == null || featureColumns.isEmpty()) {
            Messages.showWarningDialog(project, "No feature columns found for this model.", "Cannot Predict");
            return;
        }

        // Show prediction dialog (non-modal, allows multiple predictions)
        MLPredictDialog dialog = new MLPredictDialog(result, featureColumns);
        dialog.show();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MLExecutionResult target) {
        presentation.setText("Predict");
        presentation.setIcon(Icons.ACTION_EXECUTE);

        presentation.setVisible(target != null);
        presentation.setEnabled(target != null);
    }
}
