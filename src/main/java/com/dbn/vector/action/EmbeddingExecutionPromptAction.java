package com.dbn.vector.action;

import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.result.AbstractEmbeddingExecutionResultAction;
import com.dbn.vector.result.VectorEmbeddingExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class EmbeddingExecutionPromptAction extends AbstractEmbeddingExecutionResultAction {

  @Override
  protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull VectorEmbeddingExecutionResult executionResult) {
    VectorEmbeddingResult embeddingResult = executionResult.getVectorEmbeddingResult();
    VectorEmbeddingRequest embeddingRequest = embeddingResult.getRequest();
    ConnectionHandler connection = executionResult.getConnection();

    DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
    vectorManager.openVectorToolbox(connection, embeddingRequest);
  }
}
