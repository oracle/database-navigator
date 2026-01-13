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

package com.dbn.vector.ui.request;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.model.request.EmbeddingChunkingConfig;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class EmbeddingChunkLabDialog extends DBNDialog<EmbeddingChunkLabForm> {
  private final ConnectionRef connection;
  private EmbeddingChunkingConfig chunkConfig;

  public EmbeddingChunkLabDialog(ConnectionHandler connection, EmbeddingChunkingConfig chunkConfig) {
    super(connection.getProject(), "Chunk Lab", true);
    this.connection = connection.ref();
    this.chunkConfig = chunkConfig;
    renameAction(getOKAction(), "Use Configuration");
    init();
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected @NotNull EmbeddingChunkLabForm createForm() {
    return new EmbeddingChunkLabForm(this, getConnection(), chunkConfig);
  }

  @Override
  protected void doOKAction() {
    EmbeddingChunkLabForm form = getForm();
    chunkConfig =  form.getChunkConfiguration();
    super.doOKAction();
  }

}
