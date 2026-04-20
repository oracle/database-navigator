package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.util.List;


public class ToolDefinitionCreateDialog extends DBNDialog<ToolDefinitionCreateForm> {

    private final ConnectionHandler connection;
    private final ToolDefinitionModel existing;
    private final List<String> usedToolNames;

    public ToolDefinitionCreateDialog(@Nullable Project project, @NotNull ConnectionHandler connection) {
        this(project, connection, null, List.of());
    }

    public ToolDefinitionCreateDialog(@Nullable Project project, @NotNull ConnectionHandler connection, @Nullable ToolDefinitionModel existing) {
        this(project, connection, existing, List.of());
    }

    public ToolDefinitionCreateDialog(@Nullable Project project,
                                      @NotNull ConnectionHandler connection,
                                      @Nullable ToolDefinitionModel existing,
                                      @NotNull List<String> usedToolNames) {
        super(project, existing == null ? "Create MCP Tool" : "Edit MCP Tool", true);
        this.connection = connection;
        this.existing = existing;
        this.usedToolNames = usedToolNames;
        setDefaultSize(980, 740);
        init();
    }

    @Override
    protected @NotNull ToolDefinitionCreateForm createForm() {
        return new ToolDefinitionCreateForm(this, connection, existing, usedToolNames);
    }

    protected final Action[] initializeActions() {
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        ToolDefinitionCreateForm form = getForm();
        if (!form.hasPassingTestForCurrentSql()) {
            int option = Messages.showConfirmationDialog(
                    getProject(),
                    existing == null ? "Create MCP Tool" : "Edit MCP Tool",
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
