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

package com.dbn.assistant.mcp;

import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerBundle;
import com.dbn.assistant.mcp.model.AssistantMcpServerData;
import com.dbn.assistant.mcp.ui.AssistantMcpServersSettingsForm;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.EntityId;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.WorkspaceConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBoolean;

@Getter
@Setter
@WorkspaceConfig
@EqualsAndHashCode(callSuper = false)
public class AssistantMcpServerSettings
        extends BasicProjectConfiguration<AssistantSettings, AssistantMcpServersSettingsForm> {

    private AssistantMcpServerBundle mcpServers;
    private AssistantMcpServerData mcpServerData;
    private boolean workspaceIntegration;
    private final AssistantMcpToolApprovals mcpToolApprovals = new AssistantMcpToolApprovals();


    public AssistantMcpServerSettings(AssistantSettings parent) {
        super(parent);
        Project project = parent.getProject();
        mcpServers = new AssistantMcpServerBundle(project);
        mcpServerData = new AssistantMcpServerData(project);
    }

    public void setMcpServers(AssistantMcpServerBundle mcpServers) {
        this.mcpServers = new AssistantMcpServerBundle(getProject(), mcpServers.getElements());
    }

    public Set<EntityId> getMcpServerIds() {
        return mcpServers.getMcpServerIds();
    }


    @Nullable
    public AssistantMcpServer getMcpServer(EntityId serverId) {
        return mcpServers.getMcpServer(serverId);
    }


    @NotNull
    @Override
    public AssistantMcpServersSettingsForm createConfigurationEditor() {
        return new AssistantMcpServersSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "mcp-server-settings";
    }

    @Override
    public void readConfiguration(Element element) {
        workspaceIntegration = getBoolean(element, "workspace-integration", false);

        Element serversElement = element.getChild("mcp-servers");
        List<AssistantMcpServer> mcpServers = new ArrayList<>();

        List<Element> serverElements = childrenOf(serversElement, "mcp-server");
        for (Element serverElement : serverElements) {
            AssistantMcpServer mcpServer = new AssistantMcpServer();
            mcpServer.readConfiguration(serverElement);
            mcpServers.add(mcpServer);
        }
        this.mcpServers.setMcpServers(mcpServers);

        Element approvalsElement = element.getChild("mcp-tool-approvals");
        mcpToolApprovals.readState(approvalsElement);
    }

    @Override
    public void writeConfiguration(Element element) {
        setBoolean(element, "workspace-integration", workspaceIntegration);

        Element serversElement = newElement(element, "mcp-servers");
        for (AssistantMcpServer mcpServer : mcpServers.getElements()) {
            Element serverElement = newElement(serversElement, "mcp-server");
            mcpServer.writeConfiguration(serverElement);
        }

        Element approvalsElement = newElement(element, "mcp-tool-approvals");
        mcpToolApprovals.writeState(approvalsElement);
    }
}
