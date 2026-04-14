package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;


public class ToolDefinitionCreateDialog extends DBNDialog<ToolDefinitionCreateForm> {

    private final ConnectionHandler connection;
    private final ToolDefinitionModel existing;

    public ToolDefinitionCreateDialog(@Nullable Project project, @NotNull ConnectionHandler connection) {
        this(project, connection, null);
    }

    public ToolDefinitionCreateDialog(@Nullable Project project, @NotNull ConnectionHandler connection, @Nullable ToolDefinitionModel existing) {
        super(project, existing == null ? "Create MCP Tool" : "Edit MCP Tool", true);
        this.connection = connection;
        this.existing = existing;
        init();
    }

    @Override
    protected @NotNull ToolDefinitionCreateForm createForm() {
        return new ToolDefinitionCreateForm(this, connection, existing);
    }

    protected final Action[] initializeActions() {
        return actions(
                getOKAction(),
                getCancelAction());
    }
}
