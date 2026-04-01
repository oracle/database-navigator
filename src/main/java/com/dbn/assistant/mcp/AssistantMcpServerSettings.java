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

package com.dbn.assistant.mcp;

import com.dbn.assistant.mcp.ui.AssistantMcpServersSettingsForm;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.options.BasicProjectConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AssistantMcpServerSettings
        extends BasicProjectConfiguration<AssistantSettings, AssistantMcpServersSettingsForm> {

    private AssistantMcpServerBundle mcpServers;

    public AssistantMcpServerSettings(AssistantSettings parent) {
        super(parent);
        mcpServers = new AssistantMcpServerBundle(parent.getProject());
    }

    public void setMcpServers(AssistantMcpServerBundle mcpServers) {
        this.mcpServers = new AssistantMcpServerBundle(getProject(), mcpServers.getElements());
    }

    public Set<String> getMcpServerIds() {
        return mcpServers.getMcpServerIds();
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
        Element serversElement = element.getChild("mcp-servers");
        List<AssistantMcpServer> mcpServers = new ArrayList<>();

        List<Element> serverElements = childrenOf(serversElement, "mcp-server");
        for (Element serverElement : serverElements) {
            AssistantMcpServer mcpServer = new AssistantMcpServer();
            mcpServer.readConfiguration(serverElement);
            mcpServers.add(mcpServer);
        }
        this.mcpServers.setMcpServers(mcpServers);
    }

    @Override
    public void writeConfiguration(Element element) {
        Element serversElement = newElement(element, "mcp-servers");
        for (AssistantMcpServer mcpServer : mcpServers.getElements()) {
            Element serverElement = newElement(serversElement, "mcp-server");
            mcpServer.writeConfiguration(serverElement);
        }
    }
}
