/*
 * Copyright 2024 Oracle and/or its affiliates
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
import com.dbn.assistant.mcp.AssistantMcpToolInfo;
import com.dbn.common.EntityId;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.JButton;
import java.util.List;

import static com.dbn.common.util.Conditional.when;

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
                createAction("Verify", b -> verifyMcpServer(b)),
                getCancelAction());
    }

    private void verifyMcpServer(JButton button) {
        AssistantMcpServer mcpServer = new AssistantMcpServer(this.mcpServer.getId());
        getForm().applyFormChanges(mcpServer);

        Progress.modal(getProject(), null, true,
                "Verifying MCP Server configuration",
                "Verifying configuration MCP Server " + mcpServer.getName(), p -> verifyMcpServer(mcpServer));
    }

    private void openMcpToolApprovals(AssistantMcpServer mcpServer) {
        Dialogs.show(() -> new AssistantMcpToolApprovalDialog(getProject(), mcpServer));
    }

    private void verifyMcpServer(AssistantMcpServer mcpServer) {
        try {
            List<AssistantMcpToolInfo> tools = AssistantMcpServerData.loadTools(mcpServer);
            String[] options = {"Show Details", "Ok"};
            int option = Messages.showConfirmationDialog(getProject(), "MCP Server Config",
                    "Successfully verified \"" + mcpServer.getName() + "\" MCP Server configuration.\n" +
                            tools.size() + " tools found.", options, 0);
            when(option == 0, () -> openMcpToolApprovals(mcpServer));

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

