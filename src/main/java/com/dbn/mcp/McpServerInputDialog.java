package com.dbn.mcp;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.build.McpBuildManager;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class McpServerInputDialog extends DBNDialog<McpServerInputForm> {

    protected McpServerInputDialog(@Nullable Project project) {
        super(project, "MCP Builder", true);
        init();
    }

    @NotNull @Override
    protected McpServerInputForm createForm() {
        return new McpServerInputForm(this);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (!getForm().hasTools()) {
            return new ValidationInfo("Please add at least one tool definition");
        }
        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        ConnectionHandler conn = getForm().getSelectedConnection();
        String serverName = getForm().getServerName();
        List<ToolDefinitionModel> tools = getForm().getTools();
        super.doOKAction();

        new McpBuildManager(getProject(), conn, serverName, tools).execute();
    }
}
