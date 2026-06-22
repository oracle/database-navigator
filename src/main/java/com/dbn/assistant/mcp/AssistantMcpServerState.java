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
import com.dbn.assistant.mcp.model.AssistantMcpToolInfo;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.common.EntityId;
import com.dbn.common.approval.UserApprovalCancelledException;
import com.dbn.common.state.PersistentStateElement;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.dbn.assistant.mcp.ide.IdeMcpServerManager.isConflictingIdeTool;
import static com.dbn.assistant.mcp.model.AssistantMcpServerData.createToolInfo;
import static com.dbn.common.action.UserDataKeys.ASSISTANT_MCP_SERVER_STATE;
import static com.dbn.common.action.UserDataKeys.getUserDataSync;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.util.Lists.filter;


@Slf4j
@Getter
public class AssistantMcpServerState extends AssistantStateExtension implements PersistentStateElement {
    private final Map<EntityId, Boolean> selections = new ConcurrentHashMap<>();
    private int settingsSignature;

    protected AssistantMcpServerState(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantMcpServerState get(AssistantState assistantState) {
        return getUserDataSync(assistantState, ASSISTANT_MCP_SERVER_STATE,
                () -> new AssistantMcpServerState(assistantState));
    }

    private void cleanupSelections() {
        // cleanup mappings for servers which are no longer available
        AssistantMcpServerSettings mcpServerSettings = getMcpServerSettings();
        AssistantMcpServerBundle mcpServers = mcpServerSettings.getMcpServers();
        int settingsSignature = mcpServers.getSignature();
        if (settingsSignature == this.settingsSignature) return;

        this.settingsSignature = settingsSignature;
        Set<EntityId> serverIds = mcpServerSettings.getMcpServerIds();
        selections.keySet().removeIf(s -> !serverIds.contains(s));
    }

    private AssistantMcpServerSettings getMcpServerSettings() {
        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getMcpServerSettings();
    }

    public boolean isSelected(EntityId serverId) {
        Boolean selected = selections.get(serverId);
        return selected != null && selected;
    }

    public void setSelected(EntityId serverId, boolean selected) {
        selections.put(serverId, selected);
    }

    public int countSelected() {
        cleanupSelections();
        return (int) selections.values().stream().filter(b -> b).count();
    }

    private ToolExecutor createInterceptedExecutor(ToolExecutor executor) {
        return (request, memoryId) -> {
            AssistantState assistantState = getAssistantState();
            AssistantMcpServerToolInterceptor interceptor = AssistantMcpServerToolInterceptor.get(assistantState);

            return interceptor.invoke(executor, request, memoryId);
        };
    }

    public List<ToolProvider> createToolProviders(BiConsumer<String, Throwable> errorHandler) {
        Function<ToolExecutor, ToolExecutor> executor = e -> createInterceptedExecutor(e);
        BiPredicate<McpClient, ToolSpecification> filter = (c, t) -> isMcpToolAvailable(c, t);

        return getSelectedMcpServers()
                .stream()
                .map(s -> createToolProvider(s, errorHandler, filter, executor))
                .filter(p -> p != null)
                .toList();
    }

    private ToolProvider createToolProvider(
            AssistantMcpServer mcpServer,
            BiConsumer<String, Throwable> errorHandler,
            BiPredicate<McpClient, ToolSpecification> filter,
            Function<ToolExecutor, ToolExecutor> executor) {
        try {
            return AssistantMcpToolProviders.createToolProvider(mcpServer, errorHandler, filter, executor);
        } catch (UserApprovalCancelledException e) {
            // user declined; unselect so we do not prompt on every chat message
            setSelected(mcpServer.getId(), false);
            return null;
        }
    }

    private boolean isMcpToolAvailable(McpClient client, ToolSpecification specification) {
        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        AssistantMcpServerSettings serverSettings = assistantSettings.getMcpServerSettings();
        AssistantMcpServerBundle mcpServers = serverSettings.getMcpServers();

        AssistantMcpServer mcpServer = mcpServers.getMcpServer(client.key());
        if (mcpServer == null) return false;

        EntityId serverId = mcpServer.getId();
        String toolName = specification.name();
        if (mcpServer.isIdeMcpServer()) {
            if (isConflictingIdeTool(toolName)) return false;
        }

        AssistantMcpServerData mcpServerData = AssistantMcpServerData.get(project);
        AssistantMcpToolInfo toolInfo = createToolInfo(mcpServer, specification);
        mcpServerData.updateTool(toolInfo);

        AssistantMcpToolApprovals toolApprovals = serverSettings.getMcpToolApprovals();
        if (toolApprovals.isBlocked(serverId, toolInfo)) return false;

        return true;
    }

    public List<AssistantMcpServer> getSelectedMcpServers() {
        AssistantMcpServerSettings mcpServerSettings = getMcpServerSettings();
        AssistantMcpServerBundle mcpServerBundle = mcpServerSettings.getMcpServers();

        List<AssistantMcpServer> mcpServers = mcpServerBundle.getElements();
        List<AssistantMcpServer> selectedMcpServers = filter(mcpServers, e -> isSelected(e.getId()));

        AssistantMcpServer ideMcpServer = mcpServerBundle.getIdeMcpServer();
        if (ideMcpServer != null && isSelected(ideMcpServer.getId())) {
            selectedMcpServers = new ArrayList<>(selectedMcpServers);
            selectedMcpServers.add(0, ideMcpServer);
        }
        return selectedMcpServers;
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element mcpServersElement = element.getChild("selections");
        List<Element> mcpServerElements = childrenOf(mcpServersElement, "mcp-server");
        for (Element mcpServerElement : mcpServerElements) {
            EntityId serverId = constantAttribute(mcpServerElement, "id", EntityId.class);
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
            for (EntityId serverId : selections.keySet()) {
                boolean selected = selections.get(serverId);
                Element serverElement = newElement(approvalsElement, "mcp-server");
                setConstantAttribute(serverElement, "id", serverId);
                setBooleanAttribute(serverElement, "selected", selected);
            }
        }
    }
}
