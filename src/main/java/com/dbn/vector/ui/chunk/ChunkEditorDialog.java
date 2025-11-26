package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.model.chunk.ChunkConfig;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class ChunkEditorDialog extends DBNDialog<ChunkEditorForm> {
  private ConnectionRef connection;
  @Getter
  private ChunkConfig chunkConfig;

  public ChunkEditorDialog(ConnectionHandler connection, ChunkConfig chunkConfig) {
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
  protected @NotNull ChunkEditorForm createForm() {
    return new ChunkEditorForm(this, getConnection(), chunkConfig);
  }

  @Override
  protected void doOKAction() {
    ChunkEditorForm form = getForm();
    chunkConfig =  form.getChunkConfiguration();
    super.doOKAction();
  }

}
