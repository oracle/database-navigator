package com.dbn.vector.result;

import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.ExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractEmbeddingExecutionResultAction extends ContextAction<VectorEmbeddingExecutionResult> {

  protected VectorEmbeddingExecutionResult getContext(@NotNull AnActionEvent e) {
    VectorEmbeddingExecutionResult result = e.getData(DataKeys.EMBEDDING_EXECUTION_RESULT);
    if (result != null) return result;

    Project project = e.getProject();
    if (project == null) return result;

    ExecutionManager executionManager = ExecutionManager.getInstance(project);
    ExecutionResult executionResult = executionManager.getSelectedExecutionResult();
    if (executionResult instanceof VectorEmbeddingExecutionResult) {
      return (VectorEmbeddingExecutionResult) executionResult;
    }

    return null;
  }
}
