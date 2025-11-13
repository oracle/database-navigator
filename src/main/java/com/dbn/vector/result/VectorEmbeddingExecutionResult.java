package com.dbn.vector.result;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
@Getter
public class VectorEmbeddingExecutionResult extends ExecutionResultBase<VectorEmbeddingExecutionResultForm> {
  VectorEmbeddingResult vectorEmbeddingResult;

  public VectorEmbeddingExecutionResult(VectorEmbeddingResult vectorEmbeddingResult) {
    this.vectorEmbeddingResult = vectorEmbeddingResult;

  }

  @Override
  public @Nullable VectorEmbeddingExecutionResultForm createForm() {
    return new VectorEmbeddingExecutionResultForm(this);
  }

  @Override
  public @NotNull String getName() {
    return "Embedding Result";
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public @NotNull Project getProject() {
    return getConnection().getProject();
  }

  @Override
  public ConnectionId getConnectionId() {
    return getConnection().getConnectionId();
  }

  @Override
  public @NotNull ConnectionHandler getConnection() {
    return vectorEmbeddingResult.getConnection();
  }

  @Override
  public DBLanguagePsiFile createPreviewFile() {
    return null;
  }
}
