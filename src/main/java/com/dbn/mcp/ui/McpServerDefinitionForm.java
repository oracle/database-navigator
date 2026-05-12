/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.mcp.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpTransportType;
import com.dbn.mcp.util.McpServerName;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;

public class McpServerDefinitionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel toolDefinitionsPanel;
    private JBTextField serverNameField;
    private McpToolDefinitionListForm toolDefinitionListForm;
    private ComboBox<McpTransportType> transportTypeComboBox;
    private JLabel httpPortLabel;
    private JBTextField httpPortField;

    private final ConnectionHandler connection;
    private final @Getter McpServerDefinition serverDefinition;

    public McpServerDefinitionForm(@NotNull Disposable parent, @NotNull ConnectionHandler connection, @Nullable McpServerDefinition serverDefinition) {
        super(parent);
        this.connection = connection;
        this.serverDefinition = serverDefinition == null ? new McpServerDefinition() : serverDefinition;

        initInputFields();
        initToolDefinitionsPanel();

        resetFormChanges();
        initHint();
    }

    private void initInputFields() {
        initComboBox(transportTypeComboBox, McpTransportType.values());
        onSelectionChange(transportTypeComboBox, type -> {
            updateFieldAvailability();
            validateFormFields();
        });
    }

    private void initToolDefinitionsPanel() {
        toolDefinitionListForm = new McpToolDefinitionListForm(this, connection, serverDefinition);
        toolDefinitionsPanel.add(toolDefinitionListForm.getComponent());
    }

    @Override
    public void resetFormChanges() {
        setText(serverNameField, serverDefinition.getServerName());
        setSelection(transportTypeComboBox, serverDefinition.getTransportType());
        setText(httpPortField, serverDefinition.getHttpPort());
    }

    @Override
    public void applyFormChanges() {
        serverDefinition.setServerName(getText(serverNameField));
        serverDefinition.setTransportType(getSelection(transportTypeComboBox));
        serverDefinition.setHttpPort(getText(httpPortField));
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
        String html = "<html>" +
                "This will generate the Java code of a standalone MCP server with the specified tools, " +
                "as well as the self-contained JAR produced by the compilation." +
                "</html>";
        hintPanel.add(new DBNHintForm(this, TextContent.html(html), null, true).getComponent());
    }

    public String getServerName() {
        return McpServerName.normalize(serverNameField.getText());
    }

    public List<McpToolDefinition> getTools() {
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
