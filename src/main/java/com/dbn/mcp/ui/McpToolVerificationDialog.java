package com.dbn.mcp.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpToolParam;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.List;

public class McpToolVerificationDialog extends DBNDialog<McpToolVerificationForm> {
    private final ConnectionHandler connection;
    private final String statement;
    private final List<McpToolParam> params;

    public McpToolVerificationDialog(@NotNull ConnectionHandler connection,
                                     @NotNull String statement,
                                     @NotNull List<McpToolParam> params) {
        super(connection, "Test SQL Query", true);
        this.connection = connection;
        this.statement = statement;
        this.params = params;
        setDefaultSize(920, 600);
        init();
    }

    @Override
    protected @NotNull McpToolVerificationForm createForm() {
        return new McpToolVerificationForm(this, connection, statement, params);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), "Close");
        return actions(getCancelAction());
    }

    public List<McpToolParam> getParamRows() {
        return getForm().getParamRows();
    }

    public boolean isStatementVerified() {
        return getForm().isStatementVerified();
    }

    public Exception getStatementError() {
        return getForm().getStatementError();
    }
}
