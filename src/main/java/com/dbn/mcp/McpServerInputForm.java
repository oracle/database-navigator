package com.dbn.mcp;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.dbn.mcp.util.McpServerName;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

public class McpServerInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel toolDefinitionPanel;
    private JBTextField serverNameField;
    private JLabel serverNameHintLabel;
    private ToolDefinitionListForm toolDefinitionListForm;

    private final ConnectionHandler connection;

    public McpServerInputForm(@NotNull Disposable parent, @NotNull ConnectionHandler connection) {
        super(parent);
        this.connection = connection;
        serverNameField.setText("mcp-server");
        serverNameHintLabel.setText("The server name is used to identify the project that will be generated");
        serverNameHintLabel.setForeground(UIUtil.getInactiveTextColor());
        initHint();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected void initValidation() {
        addTextValidation(serverNameField, field -> McpServerName.validationError(field.getText()));
    }

    public boolean hasTools() {
        return !toolDefinitionListForm.getToolDefinitionModelList().isEmpty();
    }

    private void initHint() {
        String html = "<html><div style='font-size:11px;margin:4px 0;'>" +
                "This will generate the Java code of a standalone MCP server with the specified tools, " +
                "as well as the self-contained JAR produced by the compilation." +
                "</div></html>";
        hintPanel.add(new DBNHintForm(this, TextContent.html(html), null, true).getComponent());
    }

    private void createUIComponents() {
        toolDefinitionListForm = new ToolDefinitionListForm(this, connection);
        toolDefinitionPanel = (JPanel) toolDefinitionListForm.getComponent();
    }

    public String getServerName() {
        return McpServerName.normalize(serverNameField.getText());
    }

    public List<ToolDefinitionModel> getTools() {
        return toolDefinitionListForm.getToolDefinitionModelList();
    }
}
