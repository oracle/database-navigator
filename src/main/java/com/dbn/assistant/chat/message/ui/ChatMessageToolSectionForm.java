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
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.execution.AssistantToolInvocationMonitor;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.assistant.tool.execution.AssistantToolResponse;
import com.dbn.assistant.tool.info.AssistantToolInfoProvider;
import com.dbn.assistant.tool.info.AssistantToolInfoProviderImpl;
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
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBOptionButton;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Point;

import static com.dbn.assistant.chat.message.ChatMessageSectionType.TOOL;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.DISABLED;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Messages.showConfirmationDialog;

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
    private JBLabel toolSummaryLabel;

    private final ConnectionRef connection;
    private final ChatMessageToolSection section;
    private final AssistantToolInfoProvider info;

    ChatMessageToolSectionForm(DBNForm parent, ConnectionHandler connection, ChatMessageToolSection section) {
        super(parent, TOOL);
        this.connection = ConnectionRef.of(connection);
        this.section = section;
        framePanel.setBorder(Borders.COMPONENT_OUTLINE_BORDER);
        framePanel.setBackground(Colors.getEditorBackground());

        info = new AssistantToolInfoProviderImpl(getAssistantState(), section.getInvocation());

        initHeaderPanel();
        initActionsPanel();
        initDetailPanel();
        initConfirmationPanel();
    }

    private void initHeaderPanel() {
        toolTypeLabel.setText(info.getToolTypeName());

        toolIconLabel.setIcon(Icons.ASSISTANT_TOOL);
        toolIconLabel.setText("");

        toolInfoLabel.setText("");
        toolInfoLabel.setIcon(AllIcons.General.Note);

        String wrapperContent = TextResources.get(getClass(), "tool_info_tooltip.html.ft");
        TextContent htmlContent = TextContent.html(wrapperContent);
        htmlContent.initField("TOOL_TYPE_NAME", info.getToolTypeName());
        htmlContent.initField("TOOL_TYPE_DESCRIPTION", info.getToolTypeDescription());
        htmlContent.initField("TOOL_CATEGORY_NAME", info.getToolCategoryName());
        htmlContent.initField("TOOL_CATEGORY_DESCRIPTION", info.getToolCategoryDescription());

        String tooltipText = htmlContent.getText();
        toolInfoLabel.setToolTipText(tooltipText);
    }

    private void initDetailPanel() {
        toolNameLabel.setText(info.getToolName());

        String summary = info.getToolRequestSummary();
        String summaryTooltip = null;
        if (summary != null && summary.length() > 24) {
            summaryTooltip = summary;
            summary = StringUtil.first(summary, 24, true);
        }
        toolSummaryLabel.setText(summary);
        toolSummaryLabel.setToolTipText(summaryTooltip);
        toolSummaryLabel.setForeground(Colors.faded(UIUtil.getLabelForeground()));
    }

    private void initActionsPanel() {
        ActionToolbar chatActions = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.AssistantToolActions");
        JComponent component = chatActions.getComponent();
        component.setOpaque(false);
        this.actionsPanel.add(component);
    }

    private void initConfirmationPanel() {
        confirmationPanel.setVisible(false);
        AssistantToolInvocation toolInvocation = getToolInvocation();
        if (toolInvocation == null) return;  // old tool section (no pending execution)
        if (toolInvocation.getStatus() != AssistantToolStatus.REQUESTED) return;
        if (getInvocationMonitor() == null) return; // old incomplete tool request
        if (isPreapproved()) return;

        confirmationTextPane.setText("The agent has requested to run this tool on your database. " +
                "Please review the request and choose whether to approve or reject it. " +
                "You may also choose to always allow or deny tools of this type or category. " +
                "The system will remember your preference for future requests");

        confirmationPanel.setVisible(true);
        String toolName = info.getToolName();

        AssistantToolType toolType = info.getToolType();
        AssistantToolCategory toolCategory = info.getToolCategory();

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
                                txt("app.assistant.button.AlwaysAllowToolCategoryDesc", info.getToolCategoryName()),
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
                                txt("app.assistant.button.AlwaysDenyToolCategoryDesc", info.getToolCategoryName()),
                                () -> deny(toolCategory))));

        Layouts.horizontalBoxLayout(buttonsPanel);
        buttonsPanel.add(allowButton);
        buttonsPanel.add(denyButton);
    }

    private void allow(Object key){
        boolean confirmed = confirm(key, true);
        if (!confirmed) return;

        AssistantToolApprovals toolApprovals = getToolApprovals();
        if (key instanceof AssistantToolType) {
            AssistantToolType toolType = (AssistantToolType) key;
            toolApprovals.setStatus(toolType, APPROVED);

        } else if (key instanceof AssistantToolCategory) {
            AssistantToolCategory toolCategory = (AssistantToolCategory) key;
            toolApprovals.setStatus(toolCategory, APPROVED);
        }

        confirmationPanel.setVisible(false);
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.allow();
    }

    private void deny(Object key){
        boolean confirmed = confirm(key, false);
        if (!confirmed) return;

        AssistantToolApprovals toolApprovals = getToolApprovals();
        if (key instanceof AssistantToolType) {
            AssistantToolType toolType = (AssistantToolType) key;
            toolApprovals.setStatus(toolType, DISABLED);

        } else if (key instanceof AssistantToolCategory) {
            AssistantToolCategory toolCategory = (AssistantToolCategory) key;
            toolApprovals.setStatus(toolCategory, DISABLED);
        }

        confirmationPanel.setVisible(false);
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.deny();
    }

    private boolean confirm(Object key, boolean approval) {
        AssistantToolType toolType = null;
        AssistantToolCategory toolCategory = null;
        if (key instanceof AssistantToolType) {
            toolType = (AssistantToolType) key;
        } else if (key instanceof AssistantToolCategory) {
            toolCategory = (AssistantToolCategory) key;
        }

        if (toolType != null || toolCategory != null) {
            String title = toolType != null ?
                    approval ?
                            txt("msg.assistant.title.AlwaysAllowToolType") :
                            txt("msg.assistant.title.AlwaysDenyToolType") :
                    approval ?
                            txt("msg.assistant.title.AlwaysAllowToolCategory") :
                            txt("msg.assistant.title.AlwaysDenyToolCategory");

            String toolTypeName = getToolTypeName(toolType);
            String toolCategoryName = getToolCategoryName(toolCategory);
            String message = toolType != null ?
                    approval ?
                            txt("msg.assistant.question.AlwaysAllowToolType", toolTypeName) :
                            txt("msg.assistant.question.AlwaysDenyToolType", toolTypeName) :
                    approval ?
                            txt("msg.assistant.question.AlwaysAllowToolCategory", toolCategoryName) :
                            txt("msg.assistant.question.AlwaysDenyToolCategory", toolCategoryName);

            int option = showConfirmationDialog(
                    getProject(),
                    title,
                    message,
                    Messages.OPTIONS_YES_NO, 0);

            if (option == 1) return false;
        }
        return true;
    }

    private String getToolTypeName(AssistantToolType toolType) {
        AssistantTool assistantTool = getToolCache().getAssistantTool(toolType);
        return assistantTool == null ? "Undefined" : assistantTool.getName();
    }

    private String getToolCategoryName(AssistantToolCategory toolCategory) {
        return toolCategory == null ? "Undefined" : toolCategory.getName();
    }

    private boolean isPreapproved() {
        AssistantToolApprovals toolApprovals = getToolApprovals();
        return toolApprovals.isApproved(getAssistantTool());
    }

    public void cancelToolExecution() {
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.cancel();
    }

    public void showToolExecutionData(DataContext context) {
        Point location = getMainComponent().getLocationOnScreen();
        AssistantToolRequest toolRequest = getToolRequest();
        AssistantToolResponse toolResponse = getToolResponse();

        String request = toolRequest.getUtilityArguments();
        String response = toolResponse == null ? null : toolResponse.getContent();
        Dialogs.show(() -> new AssistantToolDataDialog(getProject(), info.getToolName(), request, response, location));
    }

    public AssistantToolInvocation getToolInvocation() {
        return section.getInvocation();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private AssistantToolRequest getToolRequest() {
        return section.getInvocation().getRequest();
    }

    private AssistantToolResponse getToolResponse() {
        return section.getInvocation().getResponse();
    }

    private AssistantTool getAssistantTool() {
        AssistantToolCache toolCache = getToolCache();

        String utilityName = getToolRequest().getUtilityName();
        return toolCache.getAssistantTool(utilityName);
    }

    private AssistantToolCache getToolCache() {
        AssistantState assistantState = getAssistantState();
        return AssistantToolCache.get(assistantState);
    }

    private AssistantToolApprovals getToolApprovals() {
        AssistantState assistantState = getAssistantState();
        AssistantToolSettings settings = AssistantToolSettings.get(assistantState);
        return settings.getApprovals();
    }

    private AssistantState getAssistantState() {
        ChatBoxForm chatBoxForm = getChatBoxForm();
        return chatBoxForm.getAssistantState();
    }

    private AssistantToolInvocationMonitor getInvocationMonitor() {
        return getToolInvocation().getMonitor();
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
