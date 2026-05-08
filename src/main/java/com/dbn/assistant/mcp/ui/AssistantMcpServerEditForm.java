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

package com.dbn.assistant.mcp.ui;

import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerData;
import com.dbn.assistant.mcp.model.AssistantMcpServerType;
import com.dbn.assistant.mcp.model.AssistantMcpToolInfo;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.assistant.mcp.model.AssistantMcpServerType.HTTP;
import static com.dbn.assistant.mcp.model.AssistantMcpServerType.STDIO;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setEmptyText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class AssistantMcpServerEditForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JBTextField nameTextField;
    private JBTextField httpUrlTextField;
    private JBTextField commandArgumentsTextField;
    private TextFieldWithBrowseButton commandTextField;
    private DBNComboBox<AssistantMcpServerType> typeComboBox;
    private JLabel httpUrlLabel;
    private JLabel commandLabel;
    private JLabel commandArgumentsLabel;
    private DBNCommentLabel commandPreviewLabel;
    private DBNHyperlinkLabel verifyLink;
    private DBNHyperlinkLabel approvalsLink;


    private final AssistantMcpServer mcpServer;

    AssistantMcpServerEditForm(AssistantMcpServerEditDialog parent) {
        super(parent);
        this.mcpServer = parent.getMcpServer();

        initComboBox(typeComboBox, AssistantMcpServerType.values());

        setEmptyText(httpUrlTextField, "http://localhost:3001/mcp-server");
        setEmptyText(commandTextField.getTextField(), "java");
        setEmptyText(commandArgumentsTextField, "-jar mcp-server.jar");

        onTextChange(commandTextField, e -> updateCommandPreview());
        onTextChange(commandArgumentsTextField, e -> updateCommandPreview());
        addSingleFileChooser(getProject(), commandTextField, "Select MCP Server executable", null);

        verifyLink.setHyperlinkText("Verify configuration");
        approvalsLink.setHyperlinkText("Tool approvals");
        onHyperlinkAccess(verifyLink, e -> verifyMcpServer());
        onHyperlinkAccess(approvalsLink, e -> openMcpToolApprovals());

        resetFormChanges();
        updateCommandPreview();
        updateFieldAvailability();

        // listeners
        onSelectionChange(typeComboBox, p -> updateFieldAvailability());
    }

    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> getSelectedServerType() == HTTP, array(httpUrlLabel, httpUrlTextField));
        fieldAdapter.initFieldsVisibility(() -> getSelectedServerType() == STDIO, array(
                commandLabel,
                commandTextField,
                commandArgumentsLabel,
                commandArgumentsTextField,
                commandPreviewLabel));
    }

    private void updateCommandPreview() {
        String command = getText(commandTextField);
        String arguments = getText(commandArgumentsTextField);
        String commandPreview;
        if (Strings.isEmptyOrSpaces(command) && Strings.isEmptyOrSpaces(arguments)) {
            commandPreview = "[command preview]";
        } else {
            commandPreview = command + " " + arguments;
        }

        commandPreviewLabel.setText(commandPreview);
    }

    private AssistantMcpServerEditRequest getRequest() {
        AssistantMcpServerEditDialog dialog = ensureParentComponent();
        return dialog.getRequest();
    }

    private AssistantMcpServer getConfigMcpServer() {
        AssistantMcpServer mcpServer = new AssistantMcpServer(this.mcpServer.getId());
        applyFormChanges(mcpServer);
        return mcpServer;
    }

    private void openMcpToolApprovals() {
        AssistantMcpServer mcpServer = getConfigMcpServer();
        Dialogs.show(() -> new AssistantMcpToolApprovalDialog(getProject(), mcpServer));
    }

    private void verifyMcpServer() {
        Progress.modal(getProject(), null, true,
                "Verifying MCP Server Configuration",
                "Connecting to \"" + mcpServer.getName() + "\" MCP Server",
                p -> doVerifyMcpServer(p));
    }

    private void doVerifyMcpServer(ProgressIndicator indicator) {
        AssistantMcpServer mcpServer = getConfigMcpServer();
        try {
            String detail = mcpServer.getType() == HTTP ?
                    "Accessing http url \"" + mcpServer.getUrl() + "\"":
                    "Invoking command \"" + mcpServer.getEndpoint() + "\"";
            indicator.setText2(detail);
            List<AssistantMcpToolInfo> tools = AssistantMcpServerData.loadTools(mcpServer);
            if (indicator.isCanceled()) return;

            int count = tools.size();
            Messages.showConfirmationDialog(getProject(), "MCP Server Config",
                    "Successfully verified \"" + mcpServer.getName() + "\" MCP Server configuration. " +
                            count + (count == 1 ? " tool" : " tools") + " found.", Messages.OPTIONS_OK, 0);
        } catch (Throwable e) {
            conditionallyLog(e);

            if (indicator.isCanceled()) return;
            Messages.showErrorDialog(getProject(), "MCP Server Config",
                    "Failed to validate \"" + mcpServer.getName() + "\" MCP Server configuration.", e);
        }
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmpty(n), "Please provide a server name");
        addTextValidation(nameTextField, n -> isNotUsed(n), "The server name is already in use");
        addTextValidation(httpUrlTextField, s -> isNotEmpty(s), "Please provide the server URL");
        addTextValidation(commandTextField.getTextField(), s -> isNotEmpty(s), "Please provide the server command executable");
    }

    @Nullable
    private AssistantMcpServerType getSelectedServerType() {
        return getSelection(typeComboBox);
    }

    public void applyFormChanges() {
        applyFormChanges(mcpServer);
    }

    public void applyFormChanges(AssistantMcpServer mcpServer) {
        mcpServer.setName(getText(nameTextField));
        mcpServer.setCommand(getText(commandTextField));
        mcpServer.setCommandArguments(getText(commandArgumentsTextField));
        mcpServer.setUrl(getText(httpUrlTextField));
        mcpServer.setType(getSelectedServerType());
    }

    public void resetFormChanges() {
        setText(nameTextField, mcpServer.getName());
        setText(commandTextField, mcpServer.getCommand());
        setText(commandArgumentsTextField, mcpServer.getCommandArguments());
        setText(httpUrlTextField, mcpServer.getUrl());
        setSelection(typeComboBox, mcpServer.getType());
    }

    private boolean isNotUsed(String name) {
        return !getRequest().getUsedNames().contains(name);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
