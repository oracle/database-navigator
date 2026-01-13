package com.dbn.vector.ui.chunking;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.model.request.EmbeddingChunkingConfig;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ChunkLabDialog extends DBNDialog<ChunkLabForm> {
  private final ConnectionRef connection;
  private EmbeddingChunkingConfig chunkConfig;

  public ChunkLabDialog(ConnectionHandler connection, EmbeddingChunkingConfig chunkConfig) {
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
  protected @NotNull ChunkLabForm createForm() {
    return new ChunkLabForm(this, getConnection(), chunkConfig);
  }

  @Override
  protected void doOKAction() {
    ChunkLabForm form = getForm();
    chunkConfig =  form.getChunkConfiguration();
    super.doOKAction();
  }

}
