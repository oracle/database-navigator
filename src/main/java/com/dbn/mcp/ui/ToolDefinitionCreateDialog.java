package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToolDefinitionCreateDialog extends DBNDialog<ToolDefinitionCreateForm> {

    private final ConnectionHandler connection;

    public ToolDefinitionCreateDialog(@Nullable Project project, @Nullable ConnectionHandler connection) {
        super(project, "Create MCP Tool", true);
        this.connection = connection;
        init();
    }

    @Override
    protected @NotNull ToolDefinitionCreateForm createForm() {
        return new ToolDefinitionCreateForm(this, connection);
    }
}
