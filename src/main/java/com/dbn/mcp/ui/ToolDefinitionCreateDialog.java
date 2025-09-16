package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToolDefinitionCreateDialog extends DBNDialog<ToolDefinitionCreateForm> {

  public ToolDefinitionCreateDialog(@Nullable Project project) {
    super(project, "Create Mcp Tool", true);

    init();
  }

  @Override
  protected @NotNull ToolDefinitionCreateForm createForm() {
    return new ToolDefinitionCreateForm(this);
  }
}
