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
import com.dbn.assistant.tool.AssistantToolData;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.assistant.tool.execution.AssistantPrompt;
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
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBOptionButton;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.border.CompoundBorder;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.chat.message.ChatMessageSectionType.TOOL;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.BLOCKED;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.icon.Icons.ASSISTANT_QUESTION;
import static com.dbn.common.text.TextContent.asHtmlContent;
import static com.dbn.common.ui.Layouts.horizontalBoxLayout;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.util.Messages.showConfirmationDialog;

public class ChatMessageToolSectionForm extends ChatMessageSectionForm{
    private JPanel mainPanel;
    private JPanel messageButtonsPanel;
    private JLabel toolNameLabel;
    private JPanel actionsPanel;
    private JPanel toolNamePanel;
    private JPanel framePanel;
    private DBNInfoLabel toolInfoLabel;
    private JPanel toolTypePanel;
    private JLabel toolTypeLabel;
    private JLabel toolIconLabel;
    private JPanel messagePanel;
    private JTextPane messageTextPane;
    private JBLabel toolSummaryLabel;
    private JPanel contentPanel;
    private JLabel headerTitleLabel;
    private JPanel headerPanel;
    private JSeparator messageSeparator;
    private JPanel processingPanel;
    private JPanel processingIconPanel;
    private JPanel toolDataPanel;
    private JTextPane descriptionTextPane;

    private final ConnectionRef connection;
    private final ChatMessageToolSection section;
    private final AssistantToolInfoProvider info;

    private AssistantToolDataForm toolDataForm;

    ChatMessageToolSectionForm(DBNForm parent, ConnectionHandler connection, ChatMessageToolSection section) {
        super(parent, TOOL);
        this.connection = ConnectionRef.of(connection);
        this.section = section;
        framePanel.setBorder(Borders.COMPONENT_OUTLINE_BORDER);
        framePanel.setBackground(Colors.getEditorBackground());

        info = new AssistantToolInfoProviderImpl(getAssistantState(), section.getInvocation());

        initHeaderPanel();
        initContentPanel();
        initActionsPanel();
        initDetailPanel();
        initMessagePanel();
        initProcessingPanel();
        initToolDataPanel(false);
    }

    private void initHeaderPanel() {
        if (isInteractive()) {
            AssistantToolInvocation invocation = getToolInvocation();
            AssistantPrompt prompt = invocation.getPrompt();
            headerTitleLabel.setText(prompt.getTitle());
            headerTitleLabel.setFont(Fonts.regular(2));
            return;
        }

        headerPanel.setVisible(false);
    }

    private void initContentPanel() {
        if (isInteractive()) {
            contentPanel.setVisible(false);
            toolIconLabel.setIcon(ASSISTANT_QUESTION);
            toolIconLabel.setText("");
            return;
        }

        toolTypeLabel.setText(info.getToolTypeName());
        //toolTypeLabel.setForeground(Colors.faded(UIUtil.getLabelForeground()));

        toolIconLabel.setIcon(Icons.ASSISTANT_TOOL);
        toolIconLabel.setText("");

        String wrapperContent = TextResources.get(getClass(), "tool_info_tooltip.html.ft");
        TextContent htmlContent = TextContent.html(wrapperContent);
        htmlContent.initField("TOOL_TYPE_NAME", info.getToolTypeName());
        htmlContent.initField("TOOL_TYPE_DESCRIPTION", info.getToolTypeDescription());
        htmlContent.initField("TOOL_CATEGORY_NAME", info.getToolCategoryName());
        htmlContent.initField("TOOL_CATEGORY_DESCRIPTION", info.getToolCategoryDescription());

        toolInfoLabel.setContent(htmlContent);
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

    private void initMessagePanel() {
        initStandardMessagePanel();
        initPromptMessagePanel();
    }

    private void initProcessingPanel() {
        processingIconPanel.add(new AsyncProcessIcon("Processing tool request"));
        processingPanel.setVisible(false);
        if (getInvocationMonitor() == null) return; // old incomplete tool request
        if (isInteractive()) return;
        if (!isPreapproved()) return;

        AssistantToolInvocation invocation = getToolInvocation();
        if (invocation.getStatus() != AssistantToolStatus.REQUESTED) return;

        processingPanel.setVisible(true);
    }

    private void initPromptMessagePanel() {
        if (!isInteractive()) return;
        messagePanel.setVisible(true);
        messageSeparator.setVisible(false);

        AssistantToolInvocation invocation = getToolInvocation();
        AssistantPrompt prompt = invocation.getPrompt();
        messageTextPane.setText(prompt.getMessage());

        initPromptMessageButtons();
    }

    private void initPromptMessageButtons() {
        if (!isInteractive()) return;

        messageButtonsPanel.removeAll();
        AssistantToolInvocation invocation = getToolInvocation();
        boolean active = invocation.getStatus() == AssistantToolStatus.REQUESTED;

        AssistantPrompt prompt = invocation.getPrompt();
        List<String> options = prompt.getOptions();

        int totalLength = options.stream()
                .mapToInt(String::length)
                .sum();


        if (totalLength > 40) {
            verticalBoxLayout(messageButtonsPanel);
        } else {
            horizontalBoxLayout(messageButtonsPanel);
        }

        for (String option : options) {
            String optionText = option
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");

            JLabel optionLabel = new JLabel(asHtmlContent(optionText));
            boolean selected = Objects.equals(option, invocation.getOption());
            boolean highlighted = active || selected;

            Color foreground = highlighted ? UIUtil.getLabelForeground() : UIUtil.getLabelDisabledForeground();
            CompoundBorder border = new CompoundBorder(UIUtil.getTextFieldBorder(), Borders.insetBorder(4, 8, 4, 8));
            optionLabel.setBorder(border);
            optionLabel.setForeground(foreground);

            if (active) {
                optionLabel.setCursor(Cursors.handCursor());
                Mouse.onMouseClick(optionLabel, MouseEvent.BUTTON1, 1, c -> consumeUserOption(option));
            }
            messageButtonsPanel.add(optionLabel);

        }
        messageButtonsPanel.setVisible(true);
    }

    private void initStandardMessagePanel() {
        if (isInteractive()) return;

        messagePanel.setVisible(false);
        AssistantToolInvocation invocation = getToolInvocation();
        if (invocation.getStatus() != AssistantToolStatus.REQUESTED) return;
        if (getInvocationMonitor() == null) return; // old incomplete tool request
        if (isPreapproved()) return;

        messageTextPane.setText("The agent has requested to run this tool on your database. " +
                "Please review the request and choose whether to allow or deny it. " +
                "You may also choose to always allow or deny tools of this type or category. " +
                "The system will remember your preference for future requests");

        messagePanel.setVisible(true);
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

/*
        TODO does "cancel" make sense? should it cancel the entire tool chain?
        JButton cancelButton = new JButton(txt("app.assistant.button.CancelTool"));
        onButtonClick(cancelButton, e -> cancelToolInvocation());
*/

        horizontalBoxLayout(messageButtonsPanel);
        messageButtonsPanel.add(allowButton);
        messageButtonsPanel.add(denyButton);
        //messageButtonsPanel.add(cancelButton);
    }

    public void initToolDataPanel(boolean visible) {
        if (visible) {
            if (toolDataForm == null) {
                toolDataForm = new AssistantToolDataForm(this, info, getToolInvocation());
                toolDataPanel.add(toolDataForm.getComponent());
            }
        }

        if (info.isExternalTool()) {
            descriptionTextPane.setVisible(false);
            toolTypePanel.setVisible(false);
        } else {
            Color faded = Colors.faded(UIUtil.getLabelForeground());
            descriptionTextPane.setText(info.getToolDescription());
            descriptionTextPane.setForeground(faded);
            descriptionTextPane.setVisible(visible);
            toolTypePanel.setVisible(!visible);
        }

        toolDataPanel.setVisible(visible);
    }

    public boolean isShowingToolData() {
        return toolDataPanel != null && toolDataPanel.isVisible();
    }

    public boolean isInteractive() {
        return getTool().isInteractive();
    }

    private void consumeUserOption(String option) {
        AssistantToolInvocation invocation = getToolInvocation();
        invocation.setOption(option);

        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        if (executionMonitor == null) return;
        executionMonitor.allow();
    }

    private void allowToolInvocation(boolean always) {
        if (always && !confirm(true)) return;

        messagePanel.setVisible(false);
        processingPanel.setVisible(true);
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.allow();
    }

    private void denyToolInvocation(boolean always) {
        if (always && !confirm(false)) return;

        messagePanel.setVisible(false);
        processingPanel.setVisible(true);
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.deny();
    }

    private void cancelToolInvocation() {
        messagePanel.setVisible(false);
        processingPanel.setVisible(false);
        AssistantToolInvocationMonitor executionMonitor = getInvocationMonitor();
        executionMonitor.cancel();
    }

    public void hideProcessingIndicator() {
        processingPanel.setVisible(false);
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
        AssistantToolApprovalStatus status = approval ? APPROVED : BLOCKED;
        if (option == 0) {
            toolApprovals.setStatus(tool.getType(), status);
            return true;
        }

        if (option == 1) {
            AssistantToolCategory category = tool.getCategory();
            toolApprovals.setStatus(category, status);

            // propagate approval to all tool types in the category
            List<AssistantToolType> toolTypes = AssistantToolData.getToolTypes(category);
            toolTypes.forEach(toolType -> toolApprovals.setStatus(toolType, status));

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

    public void toggleToolExecutionData() {
        initToolDataPanel(!toolDataPanel.isVisible());
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

        String utilityName = getToolRequest().getToolName();
        return toolCache.getAssistantTool(utilityName);
    }

    private AssistantToolCache getToolCache() {
        AssistantState assistantState = getAssistantState();
        return AssistantToolCache.get(assistantState);
    }

    private AssistantToolApprovals getToolApprovals() {
        AssistantState assistantState = getAssistantState();
        return assistantState.getToolApprovals();
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
            boolean interactive = isInteractive();
            messagePanel.setVisible(interactive);
            messageButtonsPanel.setVisible(interactive);
            if (interactive) {
                initPromptMessageButtons();
            }
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
