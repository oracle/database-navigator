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
import com.dbn.common.EntityId;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class AssistantMcpServerEditDialog extends DBNDialog<AssistantMcpServerEditForm> {
    private final AssistantMcpServer mcpServer;
    private final AssistantMcpServerEditRequest request;

    public AssistantMcpServerEditDialog(Project project, AssistantMcpServerEditRequest request) {
        super(project, request.isNewMcpServer() ?
                txt("msg.assistant.title.CreateMcpServerConfig") :
                txt("msg.assistant.title.UpdateMcpServerConfig"), true);
        this.request = request;
        this.mcpServer = initMcpServer();
        setDefaultSize(600, 400);
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
                getCancelAction());
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
