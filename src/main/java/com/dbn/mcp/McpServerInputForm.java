package com.dbn.mcp;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpTransportType;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.dbn.mcp.util.McpServerName;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class McpServerInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel toolDefinitionPanel;
    private JBTextField serverNameField;
    private JLabel serverNameHintLabel;
    private ToolDefinitionListForm toolDefinitionListForm;
    private ComboBox<McpTransportType> transportTypeComboBox;
    private JLabel httpPortLabel;
    private JBTextField httpPortField;

    private final ConnectionHandler connection;

    public McpServerInputForm(@NotNull Disposable parent, @NotNull ConnectionHandler connection) {
        super(parent);
        this.connection = connection;
        serverNameField.setText("mcp-server");
        initComboBox(transportTypeComboBox, McpTransportType.values());
        setSelection(transportTypeComboBox, McpTransportType.STDIO);
        httpPortField.setText("8080");
        serverNameHintLabel.setText("The server name is used to identify the project that will be generated");
        serverNameHintLabel.setForeground(UIUtil.getInactiveTextColor());
        onSelectionChange(transportTypeComboBox, type -> {
            updateFieldAvailability();
            validateFormFields();
        });
        initHint();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected void initValidation() {
        addTextValidation(serverNameField, field -> McpServerName.validationError(field.getText()));
        addTextValidation(httpPortField, field -> validateHttpPort(field.getText()));
    }

    @Override
    protected void initFieldAvailability() {
        getFieldAdapter().initFieldsVisibility(() -> getTransportType().isHttp(), array(httpPortLabel, httpPortField));
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

    public McpTransportType getTransportType() {
        McpTransportType type = getSelection(transportTypeComboBox);
        return type == null ? McpTransportType.STDIO : type;
    }

    public int getHttpPort() {
        String value = httpPortField.getText();
        if (value == null || value.isBlank()) return 8080;
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : 8080;
        } catch (Exception ignored) {
            return 8080;
        }
    }

    private String validateHttpPort(String value) {
        if (!getTransportType().isHttp()) return null;
        if (value == null || value.isBlank()) return "HTTP port is required for HTTP transport";

        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                return "HTTP port must be between 1 and 65535";
            }
            return null;
        } catch (NumberFormatException e) {
            return "HTTP port must be a number";
        }
    }
}
