/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.ChatMessageToolSection;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolInfo.UtilityDefinition;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.approval.AssistantToolExecutionMonitor;
import com.dbn.assistant.tool.event.AssistantToolRequest;
import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.ui.Layouts;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.ui.components.JBOptionButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Point;

import static com.dbn.assistant.chat.message.ChatMessageSectionType.TOOL;
import static com.dbn.assistant.tool.AssistantToolCache.getUtilityDefinition;
import static com.dbn.common.dispose.Failsafe.nd;

public class ChatMessageToolSectionForm extends ChatMessageSectionForm{
    private JPanel mainPanel;
    private JPanel buttonsPanel;
    private JLabel toolNameLabel;
    private JPanel actionsPanel;
    private JPanel detailsPanel;
    private JPanel framePanel;
    private JLabel toolInfoLabel;
    private JPanel contentPanel;
    private JPanel headerPanel;
    private JLabel toolTypeLabel;
    private JLabel toolIconLabel;
    private JPanel confirmationPanel;
    private JTextPane confirmationTextPane;

    private final ConnectionRef connection;
    private final ChatMessageToolSection toolSection;

    ChatMessageToolSectionForm(DBNForm parent, ConnectionHandler connection, ChatMessageToolSection toolSection) {
        super(parent, TOOL);
        this.connection = ConnectionRef.of(connection);
        this.toolSection = toolSection;
        framePanel.setBorder(Borders.COMPONENT_OUTLINE_BORDER);
        framePanel.setBackground(Colors.getEditorBackground());

        initHeaderPanel();
        initActionsPanel();
        initDetailPanel();
        initConfirmationPanel();
    }

    private void initHeaderPanel() {
        AssistantTool assistantTool = getAssistantTool();
        String toolTypeName = assistantTool.getName();

        toolTypeLabel.setText(toolTypeName);

        toolIconLabel.setIcon(Icons.ASSISTANT_TOOL);
        toolIconLabel.setText("");

        AssistantToolCategory toolCategory = getToolCategory();
        toolInfoLabel.setText("");
        toolInfoLabel.setIcon(AllIcons.General.Note);


        String wrapperContent = TextResources.get(getClass(), "tool_info_tooltip.html.ft");
        TextContent htmlContent = TextContent.html(wrapperContent);
        htmlContent.initField("TOOL_TYPE_NAME", toolTypeName);
        htmlContent.initField("TOOL_TYPE_DESCRIPTION", assistantTool.getDescription());
        htmlContent.initField("TOOL_CATEGORY_NAME", toolCategory.getName());
        htmlContent.initField("TOOL_CATEGORY_DESCRIPTION", toolCategory.getDescription());

        String tooltipText = htmlContent.getText();
        toolInfoLabel.setToolTipText(tooltipText);
    }

    private void initDetailPanel() {
        AssistantTool assistantTool = getAssistantTool();
        String toolName = getToolName();
        toolNameLabel.setText(toolName);
    }

    private void initActionsPanel() {
        ActionToolbar chatActions = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.AssistantToolActions");
        this.actionsPanel.add(chatActions.getComponent());
    }

    private void initConfirmationPanel() {
        confirmationPanel.setVisible(false);
        AssistantToolRequest toolRequest = getToolRequest();
        if (toolRequest == null) return;  // old tool section (no pending execution)
        if (toolRequest.getStatus() != AssistantToolStatus.REQUESTED) return;
        if (getExecutionMonitor() == null) return; // old incomplete tool request
        if (isPreapproved()) return;

        confirmationTextPane.setText("The agent has requested to run this tool on your database. " +
                "Please review the request and choose whether to approve or reject it. " +
                "You may also choose to always allow or deny tools of this type or category. " +
                "The system will remember your preference for future requests");

        confirmationPanel.setVisible(true);
        String toolName = getToolName();
        String toolCategoryName = getToolCategoryName();

        AssistantToolType toolType = getToolType();
        AssistantToolCategory toolCategory = getToolCategory();

        JButton allowButton = new JBOptionButton(
                createAction(
                        txt("app.assistant.button.AllowTool"),
                        txt("app.assistant.button.AllowToolDesc", toolName),
                        () -> allow(toolName)),
                createActions(
                        createAction(
                                txt("app.assistant.button.AlwaysAllowTool"),
                                txt("app.assistant.button.AlwaysAllowToolDesc", toolName),
                                () -> allow(toolType)),
                        createAction(
                                txt("app.assistant.button.AlwaysAllowToolCategory"),
                                txt("app.assistant.button.AlwaysAllowToolCategoryDesc", toolCategoryName),
                                () -> allow(toolCategory))));

        JButton denyButton = new JBOptionButton(
                createAction(
                        txt("app.assistant.button.DenyTool"),
                        txt("app.assistant.button.DenyToolDesc", toolName),
                        () -> deny(toolName)),
                createActions(
                        createAction(
                                txt("app.assistant.button.AlwaysDenyTool"),
                                txt("app.assistant.button.AlwaysDenyToolDesc", toolName),
                                () -> deny(toolType)),
                        createAction(
                                txt("app.assistant.button.AlwaysDenyToolCategory"),
                                txt("app.assistant.button.AlwaysDenyToolCategoryDesc", toolCategoryName),
                                () -> deny(toolCategory))));

        Layouts.horizontalBoxLayout(buttonsPanel);
        buttonsPanel.add(allowButton);
        buttonsPanel.add(denyButton);
    }

    private void allow(Object key){
        AssistantToolApprovals toolApproval = getToolApproval();

        if (key instanceof AssistantToolType) {
            AssistantToolType toolType = (AssistantToolType) key;
            toolApproval.allow(toolType);
        } else if (key instanceof AssistantToolCategory) {
            AssistantToolCategory toolCategory = (AssistantToolCategory) key;
            toolApproval.allow(toolCategory);
        }
        confirmationPanel.setVisible(false);
        AssistantToolExecutionMonitor executionMonitor = getExecutionMonitor();
        executionMonitor.allow();
    }

    private void deny(Object key){
        AssistantToolApprovals toolApproval = getToolApproval();
        if (toolApproval == null) return;

        if (key instanceof AssistantToolType) {
            AssistantToolType toolType = (AssistantToolType) key;
            toolApproval.deny(toolType);
        } else if (key instanceof AssistantToolCategory) {
            AssistantToolCategory toolCategory = (AssistantToolCategory) key;
            toolApproval.deny(toolCategory);
        }
        confirmationPanel.setVisible(false);
        AssistantToolExecutionMonitor executionMonitor = getExecutionMonitor();
        executionMonitor.deny();
    }


    private boolean isPreapproved() {
        AssistantToolApprovals toolApproval = getToolApproval();
        return toolApproval.isPreapproved(getAssistantTool());
    }

    public void cancelToolExecution() {
        AssistantToolExecutionMonitor executionMonitor = getExecutionMonitor();
        executionMonitor.cancel();
    }

    public void showToolExecutionData(DataContext context) {
        Point location = getMainComponent().getLocationOnScreen();
        String request = toolSection.getToolRequest().getToolArguments();
        String response = toolSection.getToolResponse();
        Dialogs.show(() -> new AssistantToolDataDialog(getProject(), getToolName(), request, response, location));
    }

    public AssistantToolRequest getToolRequest() {
        return toolSection.getToolRequest();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private String getToolName() {
        String toolName = toolSection.getToolName();

        AssistantTool tool = getAssistantTool();
        UtilityDefinition definition = getUtilityDefinition(tool, toolName);
        if (definition == null) return toolName;

        return definition.name();
    }

    private AssistantToolType getToolType() {
        AssistantTool tool = getAssistantTool();
        return tool.getType();
    }

    private AssistantToolCategory getToolCategory() {
        AssistantTool tool = getAssistantTool();
        return tool.getCategory();
    }

    private String getToolCategoryName() {
        return getToolCategory().getName();
    }


    private AssistantTool getAssistantTool() {
        AssistantToolCache toolCache = getToolCache();

        String toolName = toolSection.getToolName();
        return toolCache.getAssistantTool(toolName);
    }

    private AssistantToolCache getToolCache() {
        AssistantState assistantState = getAssistantState();
        return AssistantToolCache.get(assistantState);
    }

    private AssistantToolApprovals getToolApproval() {
        AssistantState assistantState = getAssistantState();
        return AssistantToolApprovals.get(assistantState);
    }

    private AssistantState getAssistantState() {
        ChatBoxForm chatBoxForm = getChatBoxForm();
        return chatBoxForm.getAssistantState();
    }

    private AssistantToolExecutionMonitor getExecutionMonitor() {
        return getToolRequest().getExecutionMonitor();
    }

    private ChatBoxForm getChatBoxForm() {
        return nd(getParentFrom(ChatBoxForm.class));
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Override
    protected void applyContent(TextContent content, @Nullable Language language) {

    }

    public void updateToolContent(ChatMessageToolSection section) {
        AssistantToolStatus status = section.getStatus();
        if (status != AssistantToolStatus.REQUESTED) {
            confirmationPanel.setVisible(false);
        }
        updateActionToolbars();
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.CHAT_MESSAGE_TOOL_SECTION_FORM.is(dataId)) return this;
        return null;
    }
}
