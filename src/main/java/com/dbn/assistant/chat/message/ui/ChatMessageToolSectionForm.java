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
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
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
                "Please review the request and choose whether to allow or deny it. " +
                "You may also choose to always allow or deny tools of this type or category. " +
                "The system will remember your preference for future requests");

        confirmationPanel.setVisible(true);
        String toolName = info.getToolName();

        JButton allowButton = new JBOptionButton(
                createAction(
                        txt("app.assistant.button.AllowTool"),
                        txt("app.assistant.button.AllowToolDesc", toolName),
                        () -> allowToolInvocation(false)),
                createActions(createAction(
                        txt("app.assistant.button.AlwaysAllowTool"), null,
                        () -> allowToolInvocation(true))));

        JButton denyButton = new JBOptionButton(
                createAction(
                        txt("app.assistant.button.DenyTool"),
                        txt("app.assistant.button.DenyToolDesc", toolName),
                        () -> denyToolInvocation(false)),
                createActions(createAction(
                        txt("app.assistant.button.DisableTool"), null,
                        () -> denyToolInvocation(true))));

        Layouts.horizontalBoxLayout(buttonsPanel);
        buttonsPanel.add(allowButton);
        buttonsPanel.add(denyButton);
    }

    private void allowToolInvocation(boolean always) {
        if (always && !confirm(true)) return;

        confirmationPanel.setVisible(false);
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.allow();
    }

    private void denyToolInvocation(boolean always) {
        if (always && !confirm(false)) return;

        confirmationPanel.setVisible(false);
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.deny();
    }

    private boolean confirm(boolean approval) {
        String title = approval ?
                txt("msg.assistant.title.AlwaysAllowTool") :
                txt("msg.assistant.title.DisableTool");

        AssistantTool tool = getTool();
        String toolName = tool.getName();
        String categoryName = tool.getCategory().getName();

        String message = approval ?
                txt("msg.assistant.question.AlwaysAllowTool", toolName, categoryName) :
                txt("msg.assistant.question.DisableTool", toolName, categoryName);

        String[] options = approval ?
                Messages.options(
                        txt("msg.assistant.button.AllowTool"),
                        txt("msg.assistant.button.AllowToolCategory"),
                        txt("msg.shared.button.Cancel")) :
                Messages.options(
                        txt("msg.assistant.button.DisableTool"),
                        txt("msg.assistant.button.DisableToolCategory"),
                        txt("msg.shared.button.Cancel"));

        int option = showConfirmationDialog(
                getProject(),
                title,
                message,
                options, 0);

        AssistantToolApprovals toolApprovals = getToolApprovals();
        AssistantToolApprovalStatus status = approval ? APPROVED : DISABLED;
        if (option == 0) {
            toolApprovals.setStatus(tool.getType(), status);
            return true;
        }

        if (option == 1) {
            toolApprovals.setStatus(tool.getCategory(), status);
            return true;
        }

        return false;
    }

    private boolean isPreapproved() {
        AssistantToolApprovals toolApprovals = getToolApprovals();
        return toolApprovals.isApproved(getTool());
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

    private AssistantTool getTool() {
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
