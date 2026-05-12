package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;


public class McpToolDefinitionDialog extends DBNDialog<McpToolDefinitionForm> {
    private final ConnectionHandler connection;
    private final McpServerDefinition serverDefinition;
    private final McpToolDefinition toolDefinition;

    public McpToolDefinitionDialog(@Nullable Project project,
                                   @NotNull ConnectionHandler connection,
                                   @NotNull McpServerDefinition serverDefinition,
                                   @Nullable McpToolDefinition toolDefinition) {
        super(project, toolDefinition == null ? "Create MCP Tool" : "Edit MCP Tool", true);
        this.connection = connection;
        this.serverDefinition = serverDefinition;
        this.toolDefinition = toolDefinition;
        setDefaultSize(980, 740);
        init();
    }

    @Override
    protected @NotNull McpToolDefinitionForm createForm() {
        return new McpToolDefinitionForm(this, connection, serverDefinition, toolDefinition);
    }

    protected final Action[] initializeActions() {
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        McpToolDefinitionForm form = getForm();
        form.applyFormChanges();
        if (!form.hasPassingTestForCurrentSql()) {
            int option = Messages.showConfirmationDialog(
                    getProject(),
                    toolDefinition == null ? "Create MCP Tool" : "Edit MCP Tool",
                    "SQL query is " + form.getSqlTestStatusSummary() + ".\n" +
                            "Do you want to test it now before saving?",
                    Messages.options("Test now", "Save anyway", "Cancel"),
                    0);

            if (option == 0) {
                form.openSqlTestDialog();
                return;
            }
            if (option != 1) {
                return;
            }
        }

        super.doOKAction();
    }
}
