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

import com.dbn.common.EntityId;
import com.dbn.common.component.ProjectUnit;
import com.dbn.common.sign.Signed;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        return elements.size();
    }

    public AssistantMcpServer getMcpServer(EntityId id) {
        return first(elements, c -> c.getId().equals(id));
    }

    public AssistantMcpServer getMcpServer(int index) {
        return elements.get(index);
    }

    public Set<EntityId> getMcpServerIds() {
        return elements.stream().map(s -> s.getId()).collect(Collectors.toSet());
    }

    @Override
    public int getSignature() {
        return signature.get();
    }

    @Nullable
    public AssistantMcpServer resolveMcpServer(String utilityName) {
        return first(elements, s -> s.matchesUtilityName(utilityName));
    }
}
