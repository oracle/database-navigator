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

import com.dbn.common.component.ProjectUnit;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.CollectionUtil.cloneElements;
import static com.dbn.common.util.Lists.first;

@Getter
@Setter
public class AssistantMcpServerBundle extends ProjectUnit {

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
    }

    public void addMcpServer(AssistantMcpServer server) {
        this.elements.add(server);
    }

    public int size() {
        return elements.size();
    }

    public AssistantMcpServer getMcpServer(String id) {
        return first(elements, c -> c.getId().equals(id));
    }

    public AssistantMcpServer getMcpServer(int index) {
        return elements.get(index);
    }
}
