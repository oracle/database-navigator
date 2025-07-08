package com.dbn.vector.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkEditorDialog extends DBNDialog<ChunkEditorForm> {
  ConnectionHandler connectionHandler;

  protected ChunkEditorDialog(@Nullable Project project, String title, boolean canBeParent, ConnectionHandler connection) {
    super(project, title, canBeParent);
    connectionHandler = connection;
    setDefaultSize(600, 600);

    init();
  }

  @Override
  protected @NotNull ChunkEditorForm createForm() {
    return new ChunkEditorForm(this, getProject(),connectionHandler);
  }
}
