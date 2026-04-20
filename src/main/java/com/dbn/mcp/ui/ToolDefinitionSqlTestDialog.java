package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.ParamRow;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.List;

public class ToolDefinitionSqlTestDialog extends DBNDialog<ToolDefinitionSqlTestForm> {
    private final ConnectionHandler connection;
    private final String statement;
    private final List<ParamRow> params;

    public ToolDefinitionSqlTestDialog(@NotNull ConnectionHandler connection,
                                       @NotNull String statement,
                                       @NotNull List<ParamRow> params) {
        super(connection, "Test SQL Query", true);
        this.connection = connection;
        this.statement = statement;
        this.params = params;
        setDefaultSize(920, 600);
        init();
    }

    @Override
    protected @NotNull ToolDefinitionSqlTestForm createForm() {
        return new ToolDefinitionSqlTestForm(this, connection, statement, params);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), "Close");
        return actions(getCancelAction());
    }

    public List<ParamRow> getParamRows() {
        return getForm().getParamRows();
    }

    public boolean hasVerificationRun() {
        return getForm().hasVerificationRun();
    }

    public boolean isLastVerificationSuccessful() {
        return getForm().isLastVerificationSuccessful();
    }
}
