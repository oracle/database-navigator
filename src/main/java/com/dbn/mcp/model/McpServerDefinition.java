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

package com.dbn.mcp.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Cloneable.cloneList;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class McpServerDefinition implements PersistentStateElement, Cloneable<McpServerDefinition> {
    private String serverName = "mcp-server";
    private String description;
    private McpTransportType transportType = McpTransportType.STDIO;
    private String httpPort = "8080";

    private List<McpToolDefinition> tools = new ArrayList<>();

    public Set<String> getToolNames() {
        return tools.stream().map(d -> d.getName()).collect(Collectors.toSet());
    }

    @Override
    public void readState(Element element) {
        serverName = stringAttribute(element, "server-name", serverName);
        transportType = enumAttribute(element, "transport-type", McpTransportType.class);
        description = readCdata(element.getChild("description"));

        Element toolsElement = element.getChild("tools");
        List<Element> toolElements = childrenOf(toolsElement, "tool");
        for (Element toolElement : toolElements) {
            McpToolDefinition toolDefinition = new McpToolDefinition();
            toolDefinition.readState(toolElement);
            tools.add(toolDefinition);
        }

    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "server-name", serverName);
        setEnumAttribute(element, "transport-type", transportType);
        writeCdata(newElement(element, "description"), description);

        Element toolsElement = newElement(element, "tools");
        for (McpToolDefinition tool : tools) {
            tool.writeState(newElement(toolsElement, "tool"));
        }

    }

    public void addToolDefinition(McpToolDefinition toolDefinition) {
        tools.add(toolDefinition);
    }

    public void deleteToolDefinition(McpToolDefinition toolDefinition) {
        tools.remove(toolDefinition);
    }

    @Override
    @SneakyThrows
    public McpServerDefinition clone() {
        McpServerDefinition clone = cast(super.clone());
        clone.tools = new ArrayList<>(cloneList(tools));
        return clone;
    }
}
