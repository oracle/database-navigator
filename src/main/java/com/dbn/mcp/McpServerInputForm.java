package com.dbn.mcp;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.dbn.options.ui.ProjectSettingsDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import com.intellij.ui.components.JBTextField;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Insets;
import java.util.List;

public class McpServerInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel connectionPanel;
    private JPanel hintPanel;
    private JPanel toolDefinitionPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private JButton addConnectionButton;
    private JBTextField serverNameField;
    private ToolDefinitionListForm toolDefinitionListForm;

    public McpServerInputForm(@Nullable Disposable parent) {
        super(parent);
        serverNameField.setText("mcp-server");
        initHint();
        initConnectionComboBox();
        initAddButton();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(connectionComboBox, "Please select a database connection");
        addTextValidation(serverNameField, n -> n != null && !n.trim().isEmpty(), "Please enter a server name");
    }

    public boolean hasTools() {
        return !toolDefinitionListForm.getToolDefinitionModelList().isEmpty();
    }

    private void initHint() {
        String html = "<html><div style='font-size:11px;margin:4px 0;'>" +
                "<b>Build MCP data tool</b> — turn SQL into a ready-to-run MCP server JAR. " +
                "Use <code>:param</code> placeholders, fill connection + tool info, click <b>Build</b>." +
                "</div></html>";
        hintPanel.add(new DBNHintForm(this, TextContent.html(html), null, true).getComponent());
    }

    private void initConnectionComboBox() {
        if (getProject() != null) refreshConnections();
    }

    private void initAddButton() {
        addConnectionButton.setIcon(AllIcons.General.Add);
        addConnectionButton.setToolTipText("Create new Oracle connection");
        addConnectionButton.setText("");
        addConnectionButton.setFocusable(false);
        addConnectionButton.setMargin(new Insets(2, 4, 2, 4));
        addConnectionButton.addActionListener(e -> openConnectionDialog());
    }

    private void refreshConnections() {
        Project project = getProject();
        if (project == null) return;

        List<ConnectionHandler> connections = ConnectionManager.getInstance(project).getConnections(DatabaseType.ORACLE);
        ConnectionHandler previous = connectionComboBox.getSelectedValue();
        connectionComboBox.setValues(connections);

        if (previous != null && connections.contains(previous)) {
            connectionComboBox.setSelectedValue(previous);
        } else if (!connections.isEmpty()) {
            connectionComboBox.setSelectedValue(connections.get(0));
        }
    }

    private void openConnectionDialog() {
        Project project = getProject();
        if (project == null) return;
        Dialogs.show(
                () -> new ProjectSettingsDialog(project, DatabaseType.ORACLE, ConnectionConfigType.BASIC),
                (dialog, exitCode) -> refreshConnections());
    }

    private void createUIComponents() {
        toolDefinitionListForm = new ToolDefinitionListForm(this, this::getSelectedConnection);
        toolDefinitionPanel = (JPanel) toolDefinitionListForm.getComponent();
    }

    @Nullable
    public ConnectionHandler getSelectedConnection() {
        return connectionComboBox != null ? connectionComboBox.getSelectedValue() : null;
    }

    public String getServerName() {
        return serverNameField.getText().trim();
    }

    public List<ToolDefinitionModel> getTools() {
        return toolDefinitionListForm.getToolDefinitionModelList();
    }
}
