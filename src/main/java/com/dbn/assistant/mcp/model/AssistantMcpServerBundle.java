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
import com.dbn.assistant.mcp.ide.IdeMcpServerManager;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.EntityId;
import com.dbn.common.component.ProjectUnit;
import com.dbn.common.sign.Signed;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.dbn.assistant.mcp.ide.IdeMcpServerManager.isIdeMcpPluginSupported;
import static com.dbn.common.util.CollectionUtil.cloneElements;
import static com.dbn.common.util.Lists.first;

@Getter
@Setter
public class AssistantMcpServerBundle extends ProjectUnit implements Signed {
    private final AtomicInteger signature = new AtomicInteger(0);
    private final List<AssistantMcpServer> elements = new ArrayList<>();

    public AssistantMcpServerBundle(Project project) {
        super(project);
    }

    public AssistantMcpServerBundle(Project project, List<AssistantMcpServer> servers) {
        this(project);
        setMcpServers(servers);
    }

    public void setMcpServers(List<AssistantMcpServer> servers) {
        this.elements.clear();
        cloneElements(servers, this.elements);
        updateSignature();
    }

    private void updateSignature() {
        signature.incrementAndGet();
    }

    public void addMcpServer(AssistantMcpServer server) {
        this.elements.add(server);
        updateSignature();
    }

    public int size() {
        int size = elements.size();
        if (getIdeMcpServer() != null) size++;
        return size;
    }

    public Set<EntityId> getMcpServerIds() {
        Set<EntityId> serverIds = new HashSet<>();
        for (AssistantMcpServer s : elements) {
            EntityId serverId = s.getId();
            serverIds.add(serverId);
        }
        AssistantMcpServer ideMcpServer = getIdeMcpServer();
        if (ideMcpServer != null) {
            EntityId serverId = ideMcpServer.getId();
            serverIds.add(serverId);
        }

        return serverIds;
    }

    @Override
    public int getSignature() {
        return signature.get();
    }

    @Nullable
    public AssistantMcpServer getMcpServer(EntityId id) {
        return resolveMcpServer(s -> s.getId().equals(id));
    }

    @Nullable
    public AssistantMcpServer getMcpServer(String key) {
        return resolveMcpServer(s -> s.getKey().equals(key));
    }

    @Nullable
    public AssistantMcpServer resolveMcpServer(String utilityName) {
        return resolveMcpServer(s -> s.matchesUtilityName(utilityName));
    }

    @Nullable
    private AssistantMcpServer resolveMcpServer(Predicate<AssistantMcpServer> predicate) {
        AssistantMcpServer mcpServer = first(elements, predicate);
        if (mcpServer != null) return mcpServer;

        AssistantMcpServer ideMcpServer = getIdeMcpServer();
        if (ideMcpServer == null) return null;
        if (predicate.test(ideMcpServer)) return ideMcpServer;

        return null;
    }


    @Nullable
    public AssistantMcpServer getIdeMcpServer() {
        if (!isIdeMcpPluginSupported()) return null;

        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);

        AssistantMcpServerSettings mcpServerSettings = assistantSettings.getMcpServerSettings();
        if (mcpServerSettings.isWorkspaceIntegration()) {
            IdeMcpServerManager ideMcpServerManager = IdeMcpServerManager.getInstance();
            return ideMcpServerManager.getIdeMcpServer();
        }
        return null;
    }
}
