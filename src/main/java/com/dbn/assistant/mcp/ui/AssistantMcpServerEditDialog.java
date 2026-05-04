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
import com.dbn.assistant.mcp.model.AssistantMcpToolInfo;
import com.dbn.common.EntityId;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.List;

@Getter
public class AssistantMcpServerEditDialog extends DBNDialog<AssistantMcpServerEditForm> {
    private final AssistantMcpServer mcpServer;
    private final AssistantMcpServerEditRequest request;

    public AssistantMcpServerEditDialog(Project project, AssistantMcpServerEditRequest request) {
        super(project, request.isNewMcpServer() ? "Create MCP Server Config" : "Update MCP Server Config", true);
        this.request = request;
        this.mcpServer = initMcpServer();

        setModal(true);
        setAutoSize(true);
        init();
    }

    private AssistantMcpServer initMcpServer() {
        AssistantMcpServer mcpServer = request.getMcpServer();
        if (mcpServer == null) {
            EntityId serverId = EntityId.create(false);
            mcpServer = new AssistantMcpServer(serverId);
        }
        return mcpServer;
    }

    @NotNull
    @Override
    protected AssistantMcpServerEditForm createForm() {
        return new AssistantMcpServerEditForm(this);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        String actionName = request.isNewMcpServer() ? "Create" : "Update";
        renameAction(getOKAction(), actionName);
        return actions(
                getOKAction(),
                createAction("Verify", b -> verifyMcpServer()),
                createAction("Tool Approvals", b -> openMcpToolApprovals()),
                getCancelAction());
    }

    private void verifyMcpServer() {
        AssistantMcpServer mcpServer = getConfigMcpServer();

        Progress.modal(getProject(), null, true,
                "Verifying MCP Server configuration",
                "Verifying configuration MCP Server " + mcpServer.getName(), p -> verifyMcpServer(mcpServer));
    }

    private @NotNull AssistantMcpServer getConfigMcpServer() {
        AssistantMcpServer mcpServer = new AssistantMcpServer(this.mcpServer.getId());
        getForm().applyFormChanges(mcpServer);
        return mcpServer;
    }

    private void openMcpToolApprovals() {
        AssistantMcpServer mcpServer = getConfigMcpServer();
        Dialogs.show(() -> new AssistantMcpToolApprovalDialog(getProject(), mcpServer));
    }

    private void verifyMcpServer(AssistantMcpServer mcpServer) {
        try {
            List<AssistantMcpToolInfo> tools = AssistantMcpServerData.loadTools(mcpServer);
            Messages.showConfirmationDialog(getProject(), "MCP Server Config",
                    "Successfully verified \"" + mcpServer.getName() + "\" MCP Server configuration.\n" +
                            tools.size() + " tools found.", Messages.OPTIONS_OK, 0);
        } catch (Throwable e) {
            Messages.showErrorDialog(getProject(), "MCP Server Config",
                    "Failed to validate \"" + mcpServer.getName() + "\" MCP Server configuration.", e);
        }
    }

    @Override
    public void doCancelAction() {
        close(0);
    }

    @Override
    protected void doOKAction() {
        AssistantMcpServerEditForm form = getForm();
        form.applyFormChanges();
        request.acceptMcpServer(mcpServer);
        super.doOKAction();
    }
}

