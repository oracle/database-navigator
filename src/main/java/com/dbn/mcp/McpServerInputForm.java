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
import com.dbn.mcp.models.ToolDefinitionModel;
import com.dbn.options.ui.ProjectSettingsDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

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
    private ToolDefinitionListForm toolDefinitionListForm;

    public McpServerInputForm(@Nullable Disposable parent) {
        super(parent);
        initHint();
        initConnectionComboBox();
        initAddButton();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void initHint() {
        String html = "<html><div style='font-size:11px;margin:4px 0;'>" +
                "<b>Build MCP data tool</b> — turn SQL into a ready-to-run MCP server JAR. " +
                "Use <code>:param</code> placeholders, fill connection + tool info, click <b>Build</b>." +
                "</div></html>";
        hintPanel.add(new DBNHintForm(null, TextContent.html(html), null, true).getComponent());
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

    public List<ToolDefinitionModel> getTools() {
        return toolDefinitionListForm.getToolDefinitionModelList();
    }

    // Shared types
    public enum ParamType { String, Integer, Float, Boolean, Date }

    public static class ParamRow {
        public String name;
        public ParamType type;
        public String defaultValue;
        public String description;

        public ParamRow(String name) { this(name, ParamType.String, "", ""); }
        public ParamRow(String name, ParamType type, String defaultValue) { this(name, type, defaultValue, ""); }

        public ParamRow(String name, ParamType type, String defaultValue, String description) {
            this.name = name;
            this.type = type != null ? type : ParamType.String;
            this.defaultValue = defaultValue != null ? defaultValue : "";
            this.description = description != null ? description : "";
        }
    }
}
