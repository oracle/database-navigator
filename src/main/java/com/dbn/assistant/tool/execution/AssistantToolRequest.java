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

import com.dbn.assistant.tool.AssistantToolData;
import com.dbn.common.util.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.dbn.common.util.Commons.nvl;

@Slf4j
@Getter
@Setter
public class AssistantToolRequest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String chatId;
    private String requestId;
    private String toolName;
    private String toolArguments;

    private Method method;
    private Object[] methodArguments;

    public AssistantToolRequest() {}

    public AssistantToolRequest(String chatId, String requestId, String toolName, String toolArguments) {
        this.chatId = chatId;
        this.requestId = nvl(requestId, () -> UUIDs.compact());

        this.toolName = toolName;
        this.toolArguments = toolArguments;

        this.method = AssistantToolData.getUtilityMethod(toolName);
    }

    @SneakyThrows
    public List<String> getToolArgumentNames() {
        // arguments sorted alphabetically (some providers do not return arguments in alphabetical order: arg0, arg1... aso)
        Map<String, ?> map = OBJECT_MAPPER.readValue(toolArguments, TreeMap.class);
        return new ArrayList<>(map.keySet());
    }

    @SneakyThrows
    public List<?> getToolArgumentValues() {
        // arguments sorted alphabetically (some providers do not return arguments in alphabetical order: arg0, arg1... aso)
        Map<String, ?> map = OBJECT_MAPPER.readValue(toolArguments, TreeMap.class);
        return new ArrayList<>(map.values());
    }
}
