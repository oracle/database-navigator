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

import com.dbn.assistant.mcp.AssistantMcpToolApprovals;
import com.dbn.assistant.mcp.action.AssistantMcpToolsReloadAction;
import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerData;
import com.dbn.assistant.mcp.model.AssistantMcpToolInfo;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.assistant.tool.approval.AssistantToolApprovalUtil;
import com.dbn.common.action.DataKeys;
import com.dbn.common.approval.UserApprovalManager;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Threads;
import com.dbn.common.ui.Layouts;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNFilterTextField;
import com.dbn.common.ui.misc.DBNToggleButton;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.exception.Exceptions.getLocalizedMessages;
import static com.dbn.common.ui.util.ClientProperty.HORIZONTAL_SCROLL_POLICY;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.containsIgnoreCase;
import static com.dbn.nls.NlsResources.txt;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

public class AssistantMcpToolApprovalsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel toolsPanel;
    private JScrollPane toolsScrollPane;
    private DBNFilterTextField filterTextField;
    private JPanel loadingPanel;
    private DBNToggleButton<AssistantToolApprovalStatus> statusToggle;
    private JPanel toolsHeaderPanel;
    private JLabel allToolsLabel;
    private JPanel messagePanel;
    private JTextPane messageTextPane;
    private JPanel groupStatusPanel;
    private JPanel actionsPanel;

    private final AssistantMcpServer mcpServer;
    private final List<AssistantMcpToolApprovalForm> toolForms = DisposableContainers.list(this);

    public AssistantMcpToolApprovalsForm(AssistantMcpToolApprovalDialog dialog, AssistantMcpServer mcpServer) {
        super(dialog);
        this.mcpServer = mcpServer;

        initHintPanel();
        initToolsPanel();
        initFilterField();
        initStatusToggle();

        whenFirstShown(() -> toolsScrollPane.getVerticalScrollBar().setValue(0));
    }

    private void initStatusToggle() {
        AssistantToolApprovalUtil.initStatusToggle(statusToggle,
                AssistantToolApprovalStatus.values(),
                () -> getApprovalStatus(),
                s -> setApprovalStatus(s));
    }

    private AssistantToolApprovalStatus getApprovalStatus() {
        AssistantMcpToolApprovals approvals = getToolApprovals();
        return approvals.getStatus(mcpServer.getId());
    }

    public void setApprovalStatus(AssistantToolApprovalStatus status) {
        AssistantMcpToolApprovals approvals = getToolApprovals();
        approvals.setStatus(mcpServer.getId(), status);

        refreshState();
        updateActionToolbars();
    }

    private void refreshState() {
        toolForms.forEach(toolForm -> toolForm.refreshState());
    }

    private void initFilterField() {
        filterTextField.getEmptyText().setText(txt("app.assistant.placeholder.Filter"));
        onTextChange(filterTextField, e -> filterToolForms());

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, new AssistantMcpToolsReloadAction());
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void filterToolForms() {
        for (AssistantMcpToolApprovalForm toolForm : toolForms) {
            updateToolVisibility(toolForm);
        }
    }

    private void updateToolVisibility(AssistantMcpToolApprovalForm toolForm) {
        String text = getFilterText();
        AssistantMcpToolInfo toolInfo = toolForm.getToolInfo();
        boolean visible =
                containsIgnoreCase(toolInfo.getName(), text) ||
                containsIgnoreCase(toolInfo.getDescription(), text);
        toolForm.setVisible(visible);
    }

    private String getFilterText() {
        return getText(filterTextField);
    }

    private void initHintPanel() {
        String hintContent = TextResources.getLocalizable(AssistantMcpToolApprovalsForm.class, "assistant_mcp_tool_approval.html.ft");
        TextContent hintText = TextContent.html(hintContent);
        hintText.initField("MCP_SERVER_NAME", mcpServer.getName());
        hintText.initFonts();

        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initToolsPanel() {
        HORIZONTAL_SCROLL_POLICY.set(toolsScrollPane, HORIZONTAL_SCROLLBAR_NEVER);
        Layouts.verticalBoxLayout(toolsPanel);

        loadingPanel.add(new AsyncProcessIcon(txt("cfg.assistant.text.LoadingTools")), BorderLayout.WEST);
        mainPanel.remove(toolsHeaderPanel);
        toolsScrollPane.setColumnHeaderView(toolsHeaderPanel);

        Dispatch.async(mainPanel, () -> loadTools(),
                tools -> initToolForms(tools));

    }

    private List<AssistantMcpToolInfo> loadTools() {
        loadingPanel.setVisible(true);
        actionsPanel.setVisible(false);
        groupStatusPanel.setVisible(false);
        initMessagePanel(null);

        try {
            Threads.sleep(1000);  // "reload" action appears to do nothing when tools are loading within few milliseconds
            List<AssistantMcpToolInfo> tools = AssistantMcpServerData.loadTools(mcpServer);
            groupStatusPanel.setVisible(true);
            return tools;
        } catch (Throwable e) {
            groupStatusPanel.setVisible(false);
            String message = txt("msg.assistant.error.McpToolsLoadFailed", mcpServer.getName());
            initMessagePanel(txt("msg.shared.error.ErrorDetails", message, getLocalizedMessages(e)));
            return List.of();
        } finally {
            loadingPanel.setVisible(false);
            actionsPanel.setVisible(true);
        }
    }

    public boolean isLoading() {
        return loadingPanel.isVisible();
    }

    private void initMessagePanel(String error) {
        messagePanel.setVisible(error != null);
        messageTextPane.setText(error);
        messageTextPane.setForeground(error == null ? null : UIUtil.getErrorForeground());
    }

    private void initToolForms(List<AssistantMcpToolInfo> tools) {
        List<AssistantMcpToolApprovalForm> toolForms = new ArrayList<>(this.toolForms);
        this.toolsPanel.removeAll();
        this.toolForms.clear();

        Disposer.disposeCollection(toolForms);

        for (AssistantMcpToolInfo toolInfo : tools) {
            AssistantMcpToolApprovalForm approvalForm = new AssistantMcpToolApprovalForm(this, toolInfo);
            this.toolForms.add(approvalForm);
            this.toolsPanel.add(approvalForm.getComponent());
            updateToolVisibility(approvalForm);
        }

        updateActionToolbars();
        filterToolForms();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.ASSISTANT_MCP_TOOL_APPROVALS_FORM.is(dataId)) return this;
        return null;
    }

    public AssistantMcpToolApprovals getToolApprovals() {
        Project project = ensureProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getMcpServerSettings().getMcpToolApprovals();
    }

    public void reloadTools() {
        UserApprovalManager approvalManager = UserApprovalManager.getInstance();
        approvalManager.approveTemporarily(mcpServer);

        Dispatch.async(mainPanel, () -> loadTools(),
                tools -> initToolForms(tools));
    }
}
