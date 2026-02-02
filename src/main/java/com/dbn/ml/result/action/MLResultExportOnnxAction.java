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
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.MLExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tribuo.Model;
import org.tribuo.ONNXExportable;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Action to export ML model to ONNX format.
 *
 * @author ayoub allali
 */
public class MLResultExportOnnxAction extends AbstractMLExecutionResultAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MLExecutionResult executionResult) {
        MLResult result = executionResult.getMlResult();
        Model<?> model = result.getTribuoModel();

        if (!(model instanceof ONNXExportable exportable)) {
            Messages.showWarningDialog(project, "This model type does not support ONNX export.", "Export Not Available");
            return;
        }

        FileSaverDescriptor descriptor = new FileSaverDescriptor("Export ONNX Model", "Save model as ONNX file", "onnx");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
        VirtualFileWrapper wrapper = dialog.save(result.getAlgorithmName() + "_model.onnx");

        if (wrapper == null) return;

        Path targetPath = wrapper.getFile().toPath();
        Background.run(() -> {
            try {
                // ONNX opset version 14 is widely supported
                var onnxModel = exportable.exportONNXModel(result.getAlgorithmName(), 14L);
                Files.write(targetPath, onnxModel.toByteArray());

                Dispatch.run(() -> Messages.showInfoDialog(
                        project,
                        "Model exported successfully to:\n" + targetPath,
                        "Export Complete"
                ));
            } catch (Exception ex) {
                Dispatch.run(() -> Messages.showErrorDialog(
                        project,
                        "Failed to export model: " + ex.getMessage(),
                        "Export Failed"
                ));
            }
        });
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MLExecutionResult target) {
        presentation.setText("Export ONNX");
        presentation.setIcon(Icons.ACTION_DOWNLOAD);

        boolean enabled = false;
        if (target != null) {
            MLResult result = target.getMlResult();
            if (result.getBackendType() == MLBackendType.TRIBUO) {
                Model<?> model = result.getTribuoModel();
                enabled = model instanceof ONNXExportable;
            }
        }
        presentation.setEnabled(enabled);
        presentation.setVisible(target != null && target.getMlResult().getBackendType() == MLBackendType.TRIBUO);
    }
}
