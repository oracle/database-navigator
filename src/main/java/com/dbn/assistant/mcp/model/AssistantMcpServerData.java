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

package com.dbn.assistant.mcp.model;

import com.dbn.assistant.mcp.AssistantMcpServerSettings;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.EntityId;
import com.dbn.common.component.ProjectUnit;
import com.dbn.common.state.PersistentStateElement;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.dbn.assistant.mcp.AssistantMcpToolProviders.createToolProvider;
import static com.dbn.assistant.mcp.ide.IdeMcpServerManager.isConflictingIdeTool;
import static com.dbn.common.exception.Exceptions.sneakyThrow;
import static dev.langchain4j.data.message.UserMessage.userMessage;
import static java.util.Collections.emptyList;


@Slf4j
@Getter
public class AssistantMcpServerData extends ProjectUnit implements PersistentStateElement {
    private final Map<EntityId, List<AssistantMcpToolInfo>> tools = new ConcurrentHashMap<>();

    public AssistantMcpServerData(Project project) {
        super(project);
    }

    public static AssistantMcpServerData get(Project project) {
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getMcpServerSettings().getMcpServerData();
    }

    private AssistantMcpServerSettings getMcpServerSettings() {
        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getMcpServerSettings();
    }

    @Nullable
    public AssistantMcpServer resolveMcpServer(String utilityName) {
        AssistantMcpServerSettings mcpServerSettings = getMcpServerSettings();
        return mcpServerSettings.getMcpServers().resolveMcpServer(utilityName);
    }

    public List<AssistantMcpToolInfo> getTools(EntityId serverId) {
        return tools.computeIfAbsent(serverId, id -> loadTools(id));
    }

    public void updateTool(AssistantMcpToolInfo toolInfo) {
        EntityId serverId = toolInfo.getServerId();
        List<AssistantMcpToolInfo> toolInfos = tools.getOrDefault(serverId, emptyList());
        List<AssistantMcpToolInfo> updatedToolInfos = new ArrayList<>(toolInfos);
        updatedToolInfos.removeIf(t -> t.getName().equals(toolInfo.getName()));
        updatedToolInfos.add(toolInfo);
        updatedToolInfos.sort(Comparator.comparing(AssistantMcpToolInfo::getName));
        tools.put(serverId, updatedToolInfos);
    }

    public List<AssistantMcpToolInfo> loadTools(EntityId serverId) {
        AssistantMcpServerSettings mcpServerSettings = getMcpServerSettings();
        AssistantMcpServer mcpServer = mcpServerSettings.getMcpServer(serverId);
        return loadTools(mcpServer);
    }

    public static List<AssistantMcpToolInfo> loadTools(AssistantMcpServer mcpServer) {
        return loadToolMetadata(mcpServer).getTools();
    }

    public static AssistantMcpToolMetadata loadToolMetadata(AssistantMcpServer mcpServer) {
        if (mcpServer == null) return new AssistantMcpToolMetadata();

        BiPredicate<McpClient, ToolSpecification> filter = (m, e) -> true; // no filter
        Function<ToolExecutor, ToolExecutor> executor = e -> e; // no executor override

        ToolProvider provider = createToolProvider(mcpServer, (m, e) -> sneakyThrow(e), filter, executor);
        if (provider == null) return new AssistantMcpToolMetadata();

        InvocationContext context = InvocationContext.builder().build();
        ToolProviderRequest request = ToolProviderRequest
                .builder()
                .invocationContext(context)
                .userMessage(userMessage("List available tools"))
                .build();

        ToolProviderResult result = provider.provideTools(request);

        AssistantMcpToolMetadata metadata = new AssistantMcpToolMetadata();

        for (AiServiceTool tool : result.aiServiceTools()) {
            ToolSpecification specification = tool.toolSpecification();
            if (mcpServer.isIdeMcpServer()) {
                // filter out database related tools for IDE MCP server
                String utilityName = mcpServer.unqualifiedUtilityName(specification.name());
                if (isConflictingIdeTool(utilityName)) continue;
            }

            metadata.addTool(specification, () -> createToolInfo(mcpServer, specification));
        }

        metadata.sortTools();
        return metadata;
    }

    public static AssistantMcpToolInfo createToolInfo(AssistantMcpServer mcpServer, ToolSpecification specification) {
        return AssistantMcpToolMetadataGuard.createToolInfo(mcpServer, specification);
    }

    @Override
    public void readState(Element element) {
    }

    @Override
    public void writeState(Element element) {
    }
}
