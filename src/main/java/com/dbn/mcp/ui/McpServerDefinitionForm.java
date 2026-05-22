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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Write;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.util.FileChoosers;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Titles;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpTransportType;
import com.dbn.mcp.util.McpServerName;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class McpServerDefinitionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel toolDefinitionsPanel;
    private JLabel configFileLabel;
    private JLabel httpPortLabel;
    private JBTextField configFileTextField;
    private JBTextField serverNameTextField;
    private JBTextField httpPortField;

    private McpToolDefinitionListForm toolDefinitionListForm;
    private ComboBox<McpTransportType> transportTypeComboBox;
    private DBNCommentLabel nameInfoLabel;
    private DBNHyperlinkLabel loadConfigHyperlink;
    private DBNHyperlinkLabel resetHyperlink;

    private final ConnectionRef connection;
    private VirtualFile configFile;
    private @Getter McpServerDefinition serverDefinition;

    public McpServerDefinitionForm(@NotNull McpServerDefinitionDialog parent, @NotNull ConnectionHandler connection, @Nullable McpServerDefinition serverDefinition) {
        super(parent);
        this.connection = ConnectionRef.of(connection);
        this.serverDefinition = serverDefinition == null ? new McpServerDefinition() : serverDefinition;

        this.nameInfoLabel.setVisible(false); // TODO crowded form, consider cleanup

        initHeaderPanel();
        initHintPanel();

        initInputFields();
        initToolDefinitionsPanel();
        initConfigHyperlinks();

        resetFormChanges();
        whenFirstShown(() -> updateDialogButtons());
    }


    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, getConnection());
        headerPanel.add(headerForm.getComponent());
    }

    private void initHintPanel() {
        TextContent hintContent = TextContent.plain(
                "MCP Server Builder helps you define SQL-backed MCP tools for this database connection.\n\n" +
                        "Each tool exposes a named SQL statement with typed parameters to MCP clients. Choose STDIO " +
                        "or HTTP transport and build a standalone server package with a runnable JAR, configuration, " +
                        "wallet, source project, and README.");
        hintPanel.add(new DBNHintForm(this, hintContent, null, true).getComponent());
    }

    private void initInputFields() {
        initComboBox(transportTypeComboBox, McpTransportType.values());
        onSelectionChange(transportTypeComboBox, type -> {
            updateFieldAvailability();
            validateFormFields();
        });
    }

    private void initToolDefinitionsPanel() {
        toolDefinitionListForm = new McpToolDefinitionListForm(this, getConnection(), serverDefinition);
        toolDefinitionsPanel.add(toolDefinitionListForm.getComponent());
    }

    private void initConfigHyperlinks() {
        loadConfigHyperlink.setHyperlinkText("Load Configuration");
        onHyperlinkAccess(loadConfigHyperlink, e -> selectConfigFile());

        resetHyperlink.setHyperlinkText("Reset Form");
        onHyperlinkAccess(resetHyperlink, e -> resetConfiguration());
    }

    private void resetConfiguration() {
        updateConfiguration(null, new McpServerDefinition());
    }

    private void selectConfigFile() {
        VirtualFile configFile = chooseConfigFile();
        if (configFile == null) return;

        this.configFile = configFile;
        updateConfigFileFields();
    }

    public VirtualFile chooseConfigFile() {
        VirtualFile[] selectedFiles = FileChooser.chooseFiles(configFileChooser(), null, configFile);
        if (selectedFiles.length != 1) return null;

        VirtualFile selectedFile = selectedFiles[0];
        selectedFile = loadConfigFile(selectedFile);
        return selectedFile;
    }

    private VirtualFile loadConfigFile(VirtualFile file) {
        Project project = getProject();
        try {
            File cfgFile = new File(file.getPath());
            Element configElement = JDOMUtil.load(cfgFile);
            McpServerDefinition serverDefinition = new McpServerDefinition();
            serverDefinition.readState(configElement);
            if (isValidServerDefinition(serverDefinition)) {
                updateConfiguration(file, serverDefinition);
                return file;
            } else {
                Messages.showErrorDialog(project, "Not an MCP Server Definition",
                        "\"" + file.getPath() + "\" does not look like an MCP server definition file. " +
                        "Choose an XML file created by MCP Server Builder.");
                return null;
            }
        } catch (Throwable e) {
            Messages.showErrorDialog(project, "Could not load MCP Server definition file", e);
            return null;
        }
    }

    private void updateConfiguration(VirtualFile file, McpServerDefinition serverDefinition) {
        this.configFile = file;
        this.serverDefinition = serverDefinition;
        resetFormChanges();
        updateConfigFileFields();
        rebuildToolDefinitionForms();
        updateDialogButtons();
    }

    private void rebuildToolDefinitionForms() {
        McpToolDefinitionListForm oldToolDefinitionListForm = toolDefinitionListForm;
        toolDefinitionListForm = new McpToolDefinitionListForm(this, getConnection(), serverDefinition);
        toolDefinitionsPanel.removeAll();
        toolDefinitionsPanel.add(toolDefinitionListForm.getComponent());
        revalidateForm();
        Disposer.dispose(oldToolDefinitionListForm);
    }

    private static boolean isValidServerDefinition(McpServerDefinition definition) {
        if (isNotEmpty(definition.getServerName())) return true;
        if (!definition.getTools().isEmpty()) return true;

        return false;
    }

    private void updateConfigFileFields() {
        String configFilePath = this.configFile == null ? "" : this.configFile.getPath();
        String displayConfigFilePath = Strings.truncateWithMiddleEllipsis(configFilePath, 60);

        configFileTextField.setText(displayConfigFilePath);
        if (!configFilePath.equals(displayConfigFilePath)) {
            configFileTextField.setToolTipText(configFilePath);
        }
        updateFieldAvailability();
    }

    public static @NotNull FileChooserDescriptor configFileChooser() {
        FileChooserDescriptor descriptor = FileChoosers.singleFile().
                withTitle("Select MCP Server Definition File").
                withDescription("Select an MCP Server definition file ")/*.
                withExtensionFilter("xml")*/;
        return FileChoosers.withExtensionFilter(descriptor, "xml");
    }

    private void updateDialogButtons() {
        McpServerDefinitionDialog dialog = ensureParentDialog();
        dialog.setSaveAsVisible(configFile != null);
    }

    @Override
    public void resetFormChanges() {
        setText(serverNameTextField, serverDefinition.getServerName());
        setSelection(transportTypeComboBox, serverDefinition.getTransportType());
        setText(httpPortField, serverDefinition.getHttpPort());
    }

    @Override
    public void applyFormChanges() {
        serverDefinition.setServerName(getText(serverNameTextField));
        serverDefinition.setTransportType(getSelection(transportTypeComboBox));
        serverDefinition.setHttpPort(getText(httpPortField));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Override
    protected void initValidation() {
        addTextValidation(serverNameTextField, field -> McpServerName.validationError(field.getText()));
        addTextValidation(httpPortField, field -> validateHttpPort(field.getText()));
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> getTransportType().isHttp(), array(httpPortLabel, httpPortField));
        fieldAdapter.initFieldsVisibility(() -> configFile != null, array(configFileLabel, configFileTextField));
    }

    public boolean hasTools() {
        return !toolDefinitionListForm.getToolDefinitionModelList().isEmpty();
    }

    public List<McpToolDefinition> getTools() {
        return toolDefinitionListForm.getToolDefinitionModelList();
    }

    public McpTransportType getTransportType() {
        McpTransportType type = getSelection(transportTypeComboBox);
        return type == null ? McpTransportType.STDIO : type;
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

    public void saveConfiguration() {
        if (configFile != null) {
            saveConfiguration(configFile);
        } else {
            saveConfigurationAs();
            updateDialogButtons();
        }
    }

    public void saveConfigurationAs() {
        Project project = getProject();
        String serverName = serverDefinition.getServerName();
        FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor(
                Titles.signed(txt("msg.mcpServers.title.SaveToFile")),
                txt("msg.mcpServers.info.SaveToFile", serverName), "xml");

        FileChooserFactory fileChooserFactory = FileChooserFactory.getInstance();
        FileSaverDialog fileSaverDialog = fileChooserFactory.createSaveFileDialog(fileSaverDescriptor, project);

        VirtualFileWrapper fileWrapper = fileSaverDialog.save((VirtualFile) null, serverName);
        if (fileWrapper == null) return;

        VirtualFile file = fileWrapper.getVirtualFile(true);
        if (file == null) return;

        saveConfiguration(file);
        configFile = file;
        updateConfigFileFields();
    }

    private void saveConfiguration(VirtualFile file) {
        Element element = new Element("mcp-server-definition");
        serverDefinition.writeState(element);

        String xmlString = JDOMUtil.write(element);
        byte[] content = xmlString.getBytes();

        Project project = getProject();
        Write.run(project, () -> {
            try {
                file.setBinaryContent(content);
            } catch (IOException e) {
                conditionallyLog(e);
                String fileName = file.getName();
                Messages.showErrorDialog(project,
                        txt("msg.consoles.title.CouldNotSaveToFile"),
                        txt("msg.consoles.error.CouldNotSaveToFile", fileName), e);
            }
        });
    }
}
