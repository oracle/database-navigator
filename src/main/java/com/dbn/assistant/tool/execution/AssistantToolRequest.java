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

package com.dbn.assistant.tool.execution;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.state.ProtectedContent;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.dbn.assistant.AssistantComponent.OBJECT_MAPPER;
import static com.dbn.assistant.tool.AssistantToolData.getUtilityMethod;
import static com.dbn.assistant.tool.AssistantToolData.isInternalTool;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.state.StateEncryptionScopes.ASSISTANT_TOOL_ARGUMENTS;
import static com.dbn.common.util.Commons.nvl;

@Getter
@Setter
@Slf4j
public class AssistantToolRequest implements PersistentStateElement {
    private String chatId;
    private String requestId;
    private String toolName;
    private final ProtectedContent toolArguments = new ProtectedContent(ASSISTANT_TOOL_ARGUMENTS);

    private Method method;
    private Object[] methodArguments;
    private boolean external;

    public AssistantToolRequest() {}

    public AssistantToolRequest(String chatId, String requestId, String toolName, String toolArguments) {
        this.chatId = chatId;
        this.requestId = nvl(requestId, () -> UUIDs.compact());

        this.toolName = toolName;
        setToolArguments(toolArguments);

        this.external = !isInternalTool(toolName);
        this.method = this.external ? null : getUtilityMethod(toolName);
    }

    public String getToolArguments() {
        return toolArguments.get();
    }

    public void setToolArguments(String toolArguments) {
        this.toolArguments.set(toolArguments);
    }

    @Override
    public void readState(Element element) {
        requestId = stringAttribute(element, "request-id");
        toolName = stringAttribute(element, "tool-name");

        Element argumentsElement = element.getChild("tool-arguments");
        toolArguments.readState(argumentsElement);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "request-id", requestId);
        setStringAttribute(element, "tool-name", toolName);

        toolArguments.writeState(element, "tool-arguments");
    }

    @SneakyThrows
    public List<String> getToolArgumentNames() {
        // arguments sorted alphabetically (some providers do not return arguments in alphabetical order: arg0, arg1... aso)
        Map<String, ?> map = OBJECT_MAPPER.readValue(getToolArguments(), TreeMap.class);
        return new ArrayList<>(map.keySet());
    }

    @SneakyThrows
    public List<?> getToolArgumentValues() {
        // arguments sorted alphabetically (some providers do not return arguments in alphabetical order: arg0, arg1... aso)
        Map<String, ?> map = OBJECT_MAPPER.readValue(getToolArguments(), TreeMap.class);
        return new ArrayList<>(map.values());
    }
}
