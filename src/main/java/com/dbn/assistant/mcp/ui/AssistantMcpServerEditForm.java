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
import com.dbn.common.approval.UserApprovalManager;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.list.EditableStringListForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
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
import static com.dbn.common.ui.list.ListProperty.EDITABLE;
import static com.dbn.common.ui.list.ListProperty.SORTED;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.isEmptyText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setEmptyText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Messages.showSuccessDialog;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class AssistantMcpServerEditForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JBTextField nameTextField;
    private JBTextField httpUrlTextField;
    private TextFieldWithBrowseButton commandTextField;
    private DBNComboBox<AssistantMcpServerType> typeComboBox;
    private JLabel httpUrlLabel;
    private JLabel commandLabel;
    private JLabel commandArgumentsLabel;
    private DBNCommentLabel commandPreviewLabel;
    private DBNHyperlinkLabel verifyLink;
    private DBNHyperlinkLabel approvalsLink;
    private JPanel commandArgumentsPanel;


    private EditableStringListForm commandArgumentsList;
    private final AssistantMcpServer mcpServer;

    AssistantMcpServerEditForm(AssistantMcpServerEditDialog parent) {
        super(parent);
        this.mcpServer = parent.getMcpServer();

        initComboBox(typeComboBox, AssistantMcpServerType.values());

        initEndpointFields();
        initHyperlinkFields();

        resetFormChanges();
        updateCommandPreview();
        updateFieldAvailability();

        // listeners
        onSelectionChange(typeComboBox, p -> updateFieldAvailability());
    }

    private void initEndpointFields() {
        setEmptyText(httpUrlTextField, "http://localhost:3001/mcp-server");
        onTextChange(httpUrlTextField, e -> updateFieldAvailability());

        onTextChange(commandTextField, e -> updateCommandPreview());
        //onTextChange(commandArgumentsTextField, e -> updateCommandPreview());
        addSingleFileChooser(getProject(), commandTextField, txt("msg.mcp.title.SelectMcpServerExecutable"), null);

        commandArgumentsList = new EditableStringListForm(this, null, SORTED, EDITABLE);
        commandArgumentsList.onListChanges(e -> updateCommandPreview());
        commandArgumentsPanel.add(commandArgumentsList.getComponent());
    }

    private void initHyperlinkFields() {
        verifyLink.setHyperlinkText("Verify configuration");
        approvalsLink.setHyperlinkText("Tool approvals");
        onHyperlinkAccess(verifyLink, e -> verifyMcpServer());
        onHyperlinkAccess(approvalsLink, e -> openMcpToolApprovals());
    }

    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> getSelectedServerType() == HTTP, array(httpUrlLabel, httpUrlTextField));
        fieldAdapter.initFieldsVisibility(() -> getSelectedServerType() == STDIO, array(
                commandLabel,
                commandTextField,
                commandArgumentsLabel,
                commandArgumentsPanel,
                commandPreviewLabel));

        fieldAdapter.initFieldsAvailability(() -> hasEndpointValue(), array(verifyLink, approvalsLink));
    }

    private boolean hasEndpointValue() {
        AssistantMcpServerType selectedServerType = getSelectedServerType();
        if (selectedServerType == null) return false;

        return switch (selectedServerType) {
            case HTTP -> !isEmptyText(httpUrlTextField);
            case STDIO -> !isEmptyText(commandTextField.getTextField());
        };
    }

    private void updateCommandPreview() {
        String commandPreview = buildCommandPreview();
        commandPreviewLabel.setText(commandPreview);
        updateFieldAvailability();
    }

    private String buildCommandPreview() {
        String command = getText(commandTextField);
        String arguments = String.join(" ", getCommandArguments());
        String commandPreview;
        if (Strings.isEmptyOrSpaces(command) && Strings.isEmptyOrSpaces(arguments)) {
            commandPreview = "";
        } else {
            commandPreview = command + " " + arguments;
        }
        return commandPreview;
    }

    private List<String> getCommandArguments() {
        return commandArgumentsList
                .getStringValues()
                .stream()
                .filter(s -> isNotEmptyOrSpaces(s))
                .map(s -> s.trim())
                .toList();
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
                txt("prc.assistant.title.VerifyingMcpServerConfiguration"),
                txt("prc.assistant.text.ConnectingToMcpServer", mcpServer.getName()),
                p -> doVerifyMcpServer(p));
    }

    private void doVerifyMcpServer(ProgressIndicator indicator) {
        AssistantMcpServer mcpServer = getConfigMcpServer();
        try {
            String detail = mcpServer.getType() == HTTP ?
                    txt("prc.assistant.text.AccessingMcpHttpUrl", mcpServer.getUrl()) :
                    txt("prc.assistant.text.InvokingMcpCommand", mcpServer.getEndpoint());
            indicator.setText2(detail);

            // approve this endpoint for the verify call; persistent approval happens on settings Apply
            UserApprovalManager approvalManager = UserApprovalManager.getInstance();
            approvalManager.approveTemporarily(mcpServer);

            List<AssistantMcpToolInfo> tools = AssistantMcpServerData.loadTools(mcpServer);
            if (indicator.isCanceled()) return;

            int count = tools.size();
            String messageKey = count == 1 ?
                    "msg.assistant.info.McpServerConfigVerifiedOne" :
                    "msg.assistant.info.McpServerConfigVerifiedMany";
            showSuccessDialog(getProject(),
                    txt("msg.assistant.title.McpServerConfig"),
                    txt(messageKey, mcpServer.getName(), count));
        } catch (Throwable e) {
            conditionallyLog(e);

            if (indicator.isCanceled()) return;
            showErrorDialog(getProject(),
                    txt("msg.assistant.title.McpServerConfig"),
                    txt("msg.assistant.error.McpServerConfigValidationFailed", mcpServer.getName()), e);
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
        mcpServer.setType(getSelectedServerType());
        mcpServer.setName(getText(nameTextField));
        mcpServer.setUrl(getText(httpUrlTextField));
        mcpServer.setCommand(getText(commandTextField));
        mcpServer.setCommandArguments(getCommandArguments());
    }

    public void resetFormChanges() {
        setSelection(typeComboBox, mcpServer.getType());
        setText(nameTextField, mcpServer.getName());
        setText(httpUrlTextField, mcpServer.getUrl());
        setText(commandTextField, mcpServer.getCommand());
        commandArgumentsList.setStringValues(mcpServer.getCommandArguments());
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
