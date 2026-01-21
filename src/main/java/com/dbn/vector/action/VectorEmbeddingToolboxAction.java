package com.dbn.vector.action;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.result.VectorEmbeddingExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VectorEmbeddingToolboxAction extends AbstractVectorEmbeddingResultAction {

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
    presentation.setText("Open Vector Toolbox");
    presentation.setIcon(Icons.EXEC_RESULT_INPUT_FORM);
  }
}
