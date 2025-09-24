package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class ChunkEditorDialog extends DBNDialog<ChunkEditorForm> {
  private ConnectionRef connection;
  @Getter
  private ChunkConfiguration chunkConfiguration;

  public ChunkEditorDialog(ConnectionHandler connection, ChunkConfiguration chunkConfiguration) {
    super(connection.getProject(), "Chunk Lab", true);
    this.connection = connection.ref();
    this.chunkConfiguration = chunkConfiguration;
    renameAction(getOKAction(), "Use Configuration");
    init();
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected @NotNull ChunkEditorForm createForm() {
    return new ChunkEditorForm(this, getConnection(), chunkConfiguration);
  }

  @Override
  protected void doOKAction() {
    ChunkEditorForm form = getForm();
    chunkConfiguration =  form.getChunkConfiguration();
    super.doOKAction();
  }

}
