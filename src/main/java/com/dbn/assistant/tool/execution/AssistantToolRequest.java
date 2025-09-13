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

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;

import static com.dbn.common.util.Commons.nvl;

@Getter
@Setter
public class AssistantToolRequest {
    private String chatId;
    private String requestId;
    private String toolName;
    private String toolArguments;

    private AssistantTool tool;

    private Method method;
    private Object[] arguments;

    public AssistantToolRequest() {}

    public AssistantToolRequest(AssistantToolCache toolCache, String chatId, String requestId, String toolName, String toolArguments) {
        this.chatId = chatId;
        this.requestId = nvl(requestId, () -> UUIDs.compact());
        this.toolName = toolName;
        this.toolArguments = toolArguments;

        this.tool = toolCache.getAssistantTool(toolName);
        this.method = AssistantToolCache.getUtilityMethod(tool, toolName);
    }

    public void verify(Method method) {
        if (!this.method.equals(method)) {
            throw new IllegalArgumentException("The method to verify does not match the current request");
        }
    }
}
