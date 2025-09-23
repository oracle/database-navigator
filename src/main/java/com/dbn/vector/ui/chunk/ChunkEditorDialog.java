package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkEditorDialog extends DBNDialog<ChunkEditorForm> {
  ConnectionHandler connectionHandler;
  @Getter
  private ChunkConfiguration chunkConfiguration;

  public ChunkEditorDialog(@Nullable Project project, boolean canBeParent, ConnectionHandler connection, ChunkConfiguration chunkConfiguration) {
    super(project, "Chunk Lab", canBeParent);
    this.chunkConfiguration = chunkConfiguration;
    this.connectionHandler = connection;
    renameAction(getOKAction(), "Use Configuration");
    init();
  }

  @Override
  protected @NotNull ChunkEditorForm createForm() {
    return new ChunkEditorForm(this, getProject(),connectionHandler,chunkConfiguration);
  }

  @Override
  protected void doOKAction() {
    ChunkEditorForm form = getForm();
    chunkConfiguration =  form.getChunkConfiguration();
    super.doOKAction();
  }

}
