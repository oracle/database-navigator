package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.nls.NlsResources;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.common.util.Messages.options;


public class McpToolDefinitionDialog extends DBNDialog<McpToolDefinitionForm> {
    private final ConnectionHandler connection;
    private final McpServerDefinition serverDefinition;
    private final McpToolDefinition toolDefinition;

    public McpToolDefinitionDialog(@Nullable Project project,
                                   @NotNull ConnectionHandler connection,
                                   @NotNull McpServerDefinition serverDefinition,
                                   @Nullable McpToolDefinition toolDefinition) {
        super(project, toolDefinition == null ? NlsResources.txt("msg.mcp.title.CreateMcpTool") : NlsResources.txt("msg.mcp.title.EditMcpTool"), true);
        this.connection = connection;
        this.serverDefinition = serverDefinition;
        this.toolDefinition = toolDefinition;
        setDefaultSize(800, 600);
        renameAction(getOKAction(), toolDefinition == null ? "Add Tool" : "Save Tool");
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
        if (!form.isStatementVerified()) {
            int option = Messages.showConfirmationDialog(
                    getProject(),
                    toolDefinition == null ? txt("msg.mcp.title.CreateMcpTool") : txt("msg.mcp.title.EditMcpTool"),
                    txt("msg.mcp.question.VerifyToolStatementBeforeSaving"),
                    options(txt("msg.mcp.button.VerifyStatement"), txt("msg.mcp.button.SaveTool"), txt("msg.shared.button.Cancel")),
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
