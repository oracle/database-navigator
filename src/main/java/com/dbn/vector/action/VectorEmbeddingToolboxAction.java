/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.vector.action;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.options.general.WorkspaceFeature;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingExecutionResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class VectorEmbeddingToolboxAction extends AbstractVectorEmbeddingResultAction {

  public VectorEmbeddingToolboxAction() {
    super(txt("app.execution.action.VectorEmbeddingResultOpenVectorToolbox"));
  }

  @Override
  protected WorkspaceFeature getFeature() {
    return WorkspaceFeature.VECTOR_TOOLBOX;
  }

  @Override
  protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull VectorEmbeddingExecutionResult executionResult) {
    VectorEmbeddingResult embeddingResult = executionResult.getVectorEmbeddingResult();
    VectorEmbeddingRequest embeddingRequest = embeddingResult.getRequest();
    ConnectionHandler connection = executionResult.getConnection();

    DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
    vectorManager.openVectorToolbox(connection, embeddingRequest);
  }

  @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable VectorEmbeddingExecutionResult target) {
        presentation.setText(txt("app.vector.action.OpenVectorToolbox"));
        presentation.setIcon(Icons.EXEC_RESULT_INPUT_FORM);
        presentation.setVisible(target != null && isFeatureEnabled(project));
  }
}
