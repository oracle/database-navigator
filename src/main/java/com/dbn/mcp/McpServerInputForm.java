package com.dbn.mcp;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

public class McpServerInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel toolDefinitionPanel;
    private JBTextField serverNameField;
    private ToolDefinitionListForm toolDefinitionListForm;

    private final ConnectionHandler connection;

    public McpServerInputForm(@NotNull Disposable parent, @NotNull ConnectionHandler connection) {
        super(parent);
        this.connection = connection;
        serverNameField.setText("mcp-server");
        initHint();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected void initValidation() {
        addTextValidation(serverNameField, n -> n != null && !n.trim().isEmpty(), "Please enter a server name");
    }

    public boolean hasTools() {
        return !toolDefinitionListForm.getToolDefinitionModelList().isEmpty();
    }

    private void initHint() {
        String html = "<html><div style='font-size:11px;margin:4px 0;'>" +
                "<b>Build MCP data tool</b> — turn SQL into a ready-to-run MCP server JAR. " +
                "Use <code>:param</code> placeholders, fill in the tool info, click <b>Build</b>." +
                "</div></html>";
        hintPanel.add(new DBNHintForm(this, TextContent.html(html), null, true).getComponent());
    }

    private void createUIComponents() {
        toolDefinitionListForm = new ToolDefinitionListForm(this, connection);
        toolDefinitionPanel = (JPanel) toolDefinitionListForm.getComponent();
    }

    public String getServerName() {
        return serverNameField.getText().trim();
    }

    public List<ToolDefinitionModel> getTools() {
        return toolDefinitionListForm.getToolDefinitionModelList();
    }
}
