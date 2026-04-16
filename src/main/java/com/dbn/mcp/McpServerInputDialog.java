package com.dbn.mcp;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.build.McpBuildTask;
import com.dbn.mcp.model.McpTransportType;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.util.List;

public class McpServerInputDialog extends DBNDialog<McpServerInputForm> {

    private final ConnectionHandler connection;

    protected McpServerInputDialog(@NotNull ConnectionHandler connection) {
        super(connection, "MCP Builder", true);
        this.connection = connection;
        init();
    }

    @NotNull @Override
    protected McpServerInputForm createForm() {
        return new McpServerInputForm(this, connection);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (!getForm().hasTools()) {
            return new ValidationInfo("Please add at least one tool definition");
        }
        return super.doValidate();
    }

    protected final Action[] initializeActions() {
        renameAction(getOKAction(), "Build");
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        String serverName = getForm().getServerName();
        McpTransportType transportType = getForm().getTransportType();
        int httpPort = getForm().getHttpPort();
        List<ToolDefinitionModel> tools = getForm().getTools();
        super.doOKAction();

        new McpBuildTask(getProject(), connection, serverName, transportType, httpPort, tools).execute();
    }
}
