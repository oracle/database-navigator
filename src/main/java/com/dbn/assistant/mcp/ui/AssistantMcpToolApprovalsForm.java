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

package com.dbn.assistant.mcp.ui;

import com.dbn.assistant.mcp.AssistantMcpServer;
import com.dbn.assistant.mcp.AssistantMcpServerData;
import com.dbn.assistant.mcp.AssistantMcpToolApprovals;
import com.dbn.assistant.mcp.AssistantMcpToolInfo;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.ui.Layouts;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.ClientProperty.HORIZONTAL_SCROLL_POLICY;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

public class AssistantMcpToolApprovalsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel toolsPanel;
    private JScrollPane toolsScrollPane;

    private final AssistantMcpServer mcpServer;
    private final Map<String, AssistantMcpToolApprovalForm> toolForms = ContainerUtil.createConcurrentWeakValueMap();

    public AssistantMcpToolApprovalsForm(AssistantMcpToolApprovalDialog dialog, AssistantMcpServer mcpServer) {
        super(dialog);
        this.mcpServer = mcpServer;

        initHintPanel();
        initToolsPanel();

        whenFirstShown(() -> toolsScrollPane.getVerticalScrollBar().setValue(0));
    }

    private void initHintPanel() {
        String hintContent = TextResources.get(AssistantMcpToolApprovalsForm.class, "assistant_mcp_tool_approval.html.ft");
        TextContent hintText = TextContent.html(hintContent);
        hintText.initField("MCP_SERVER_NAME", mcpServer.getName());
        hintText.initFonts();

        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initToolsPanel() {
        HORIZONTAL_SCROLL_POLICY.set(toolsScrollPane, HORIZONTAL_SCROLLBAR_NEVER);
        Layouts.verticalBoxLayout(toolsPanel);

        AssistantMcpServerData serverData = AssistantMcpServerData.get(ensureProject());
        List<AssistantMcpToolInfo> tools = serverData.getTools(mcpServer.getId());

        for (AssistantMcpToolInfo toolInfo : tools) {
            AssistantMcpToolApprovalForm approvalForm = new AssistantMcpToolApprovalForm(this, toolInfo);
            toolForms.put(toolInfo.getName(), approvalForm);
            toolsPanel.add(approvalForm.getComponent());
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public AssistantMcpToolApprovals getToolApprovals() {
        Project project = ensureProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getMcpServerSettings().getMcpToolApprovals();
    }
}
