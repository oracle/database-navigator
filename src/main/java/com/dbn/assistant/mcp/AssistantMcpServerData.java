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

import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.common.state.PersistentStateElement;
import com.intellij.openapi.project.Project;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static com.dbn.assistant.mcp.AssistantMcpServer.qualifiedUtilityName;
import static com.dbn.common.action.UserDataKeys.ASSISTANT_MCP_SERVER_DATA;
import static com.dbn.common.action.UserDataKeys.getUserDataSync;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.filter;


@Slf4j
@Getter
public class AssistantMcpServerData extends AssistantStateExtension implements PersistentStateElement {
    private final Map<String, Boolean> selections = new ConcurrentHashMap<>();
    private int settingsSignature;

    protected AssistantMcpServerData(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantMcpServerData get(AssistantState assistantState) {
        return getUserDataSync(assistantState, ASSISTANT_MCP_SERVER_DATA,
                () -> new AssistantMcpServerData(assistantState));
    }

    private void cleanupSelections() {
        // cleanup mappings for servers which are no longer available
        AssistantMcpServerSettings mcpServerSettings = getMcpServerSettings();
        AssistantMcpServerBundle mcpServers = mcpServerSettings.getMcpServers();
        int settingsSignature = mcpServers.getSignature();
        if (settingsSignature == this.settingsSignature) return;

        this.settingsSignature = settingsSignature;
        Set<String> serverIds = mcpServerSettings.getMcpServerIds();
        selections.keySet().removeIf(s -> !serverIds.contains(s));
    }

    private AssistantMcpServerSettings getMcpServerSettings() {
        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getMcpServerSettings();
    }

    public boolean isSelected(String id) {
        Boolean selected = selections.get(id);
        return selected != null && selected;
    }

    public void setSelected(String id, boolean selected) {
        selections.put(id, selected);
    }

    public int countSelected() {
        cleanupSelections();
        return (int) selections.values().stream().filter(b -> b).count();
    }

    public List<AssistantMcpServer> getSelectedMcpServers() {
        AssistantMcpServerSettings mcpServerSettings = getMcpServerSettings();
        AssistantMcpServerBundle mcpServers = mcpServerSettings.getMcpServers();
        return filter(mcpServers.getElements(), e -> isSelected(e.getId()));
    }

    @Nullable
    public AssistantMcpServer resolveMcpServer(String utilityName) {
        AssistantMcpServerSettings mcpServerSettings = getMcpServerSettings();
        return mcpServerSettings.getMcpServers().resolveMcpServer(utilityName);
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element mcpServersElement = element.getChild("selections");
        List<Element> mcpServerElements = childrenOf(mcpServersElement, "mcp-server");
        for (Element mcpServerElement : mcpServerElements) {
            String serverId = stringAttribute(mcpServerElement, "id");
            boolean selected = booleanAttribute(mcpServerElement, "selected", false);
            selections.put(serverId, selected);
        }
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;
        cleanupSelections();

        if (!selections.isEmpty()) {
            Element approvalsElement = newElement(element, "selections");
            for (String serverId : selections.keySet()) {
                boolean selected = selections.get(serverId);
                Element serverElement = newElement(approvalsElement, "mcp-server");
                setStringAttribute(serverElement, "id", serverId);
                setBooleanAttribute(serverElement, "selected", selected);
            }
        }
    }

    private static ToolProvider createToolProvider(AssistantMcpServer mcpServer, BiConsumer<String, Throwable> errorHandler) {
        String serverName = mcpServer.getName();
        try {
            McpTransport transport = createMcpTransport(mcpServer);
            McpClient mcpClient = DefaultMcpClient.builder()
                    .key(mcpServer.getKey())
                    .transport(transport)
                    .build();

            return McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .toolNameMapper((c, s) -> qualifiedUtilityName(c.key(), s.name()))
                    .build();
        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
            errorHandler.accept("Failed to initialize MCP Server \"" + serverName + "\"", t);
            return null;
        }
    }

    private static McpTransport createMcpTransport(AssistantMcpServer mcpServer) {
        AssistantMcpServerType type = mcpServer.getType();
        return switch (type) {
            case HTTP -> StreamableHttpMcpTransport.builder()
                    .url(mcpServer.getUrl())
                    .build();
            case STDIO ->  StdioMcpTransport.builder()
                    .command(Arrays.stream(mcpServer.getCommand().split(" ")).toList()).build();
        };
    }

    public List<ToolProvider> createToolProviders(BiConsumer<String, Throwable> errorHandler) {
        return getSelectedMcpServers()
                .stream()
                .map(s -> createToolProvider(s, errorHandler))
                .filter(p -> p != null)
                .toList();
    }
}
