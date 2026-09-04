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

package com.dbn.vector.model;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.ExecutionCancellationAdapter;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.vector.ui.result.EmbeddingResultForm;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@Getter
public class VectorEmbeddingExecutionResult extends ExecutionResultBase<EmbeddingResultForm> {
  private  String name;
  VectorEmbeddingResult vectorEmbeddingResult;

  public VectorEmbeddingExecutionResult(VectorEmbeddingResult vectorEmbeddingResult,String name) {
    this.vectorEmbeddingResult = vectorEmbeddingResult;
    this.name = name;
  }

  @Override
  public EmbeddingResultForm createForm() {
    return new EmbeddingResultForm(this);
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public Icon getIcon() {
    return Icons.VECTOR_TOOLBOX;
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

  @Override
  public boolean isRenameable() {
    //todo does not support sticky naming
    return true;
  }
  @Override
  public void setName(@NotNull String name, boolean sticky) {
    this.name = name;
  }

  @Override
  public ExecutionCancellationAdapter getCancellationAdapter() {
    return vectorEmbeddingResult.getStatus() == VectorEmbeddingResult.Status.RUNNING
            ? new VectorEmbeddingCancellationAdapter(vectorEmbeddingResult)
            : null;
  }
}
