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

import com.dbn.assistant.tool.AssistantToolInfo.ParamSpec;
import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.common.Reflection;
import com.dbn.common.data.Data;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

import static com.dbn.assistant.tool.AssistantToolData.isInteractiveTool;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readSensitiveData;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeSensitiveData;
import static com.dbn.common.util.Commons.nvl;

@Getter
@Setter
public class AssistantToolInvocation implements PersistentStateElement {
    private static final @NonNls String TOOL_ARGUMENTS_ENC_SCOPE = "assistant.tool.arguments";
    private static final @NonNls String TOOL_RESPONSE_ENC_SCOPE = "assistant.tool.response";
    private static final ThreadLocal<AssistantToolInvocation> CURRENT = new ThreadLocal<>();

    private AssistantToolRequest request;
    private AssistantToolResponse response;
    private AssistantPrompt prompt;
    private String option;

    private AssistantToolStatus status = AssistantToolStatus.REQUESTED;
    private AssistantToolInvocationMonitor monitor;

    public AssistantToolInvocation() {}

    public AssistantToolInvocation(AssistantToolRequest request) {
        this.request = request;
        CURRENT.set(this); // active invocation
    }

    public boolean isInteractiveRequest() {
        return isInteractiveTool(request.getToolName());
    }

    public Object getOptionValue() {
        if (option == null) return null;
        if (prompt == null) return null;

        Method method = getRequest().getMethod();
        Class<?> returnType = method.getReturnType();
        if (option.getClass().equals(returnType)) return option;

        int parameterIndex = prompt.getParameterIndex(option);
        ParamSpec[] parameterAnnotations = Reflection.getParameterAnnotations(method, ParamSpec.class);
        ParamSpec paramSpec = parameterAnnotations[parameterIndex];
        Object value = paramSpec == null ? option : paramSpec.value();

        return Data.asType(value, returnType);
    }

    @Nullable
    public static AssistantToolInvocation current() {
        // NOTE: this assumes the tool concurrency is disabled
        // TODO find alternative ways to propagate this context to the com.dbn.assistant.tool.event.AssistantToolInvocationHandler
        return CURRENT.get();
    }

    public static void resetCurrent() {
        CURRENT.remove();
    }

    public synchronized AssistantPrompt getPrompt() {
        if (prompt != null) return prompt;
        if (isInteractiveRequest()) {
            prompt = new AssistantPrompt(request);
        }
        return prompt;
    }

    @Override
    public void readState(Element element) {
        request = new AssistantToolRequest();

        request.setRequestId(stringAttribute(element, "request-id"));
        request.setToolName(stringAttribute(element, "tool-name"));
        option = stringAttribute(element, "tool-option");
        status = enumAttribute(element, "tool-status", status);

        Element argumentsElement = element.getChild("tool-arguments");
        request.setToolArguments(readSensitiveData(argumentsElement, TOOL_ARGUMENTS_ENC_SCOPE));

        Element responseElement = element.getChild("tool-response");
        if (responseElement != null) {
            String toolResponse = readSensitiveData(responseElement, TOOL_RESPONSE_ENC_SCOPE);
            response = new AssistantToolResponse(toolResponse);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "request-id", request.getRequestId());
        setStringAttribute(element, "tool-name", request.getToolName());
        setStringAttribute(element, "tool-option", option);
        setEnumAttribute(element, "tool-status", status);

        Element contentElement = newElement(element,"tool-arguments");
        writeSensitiveData(contentElement, request.getToolArguments(), TOOL_ARGUMENTS_ENC_SCOPE);

        if (response != null) {
            Element resposeElement = newElement(element,"tool-response");
            writeSensitiveData(resposeElement, response.getContent(), TOOL_RESPONSE_ENC_SCOPE);
        }
    }

    public String getRequestContent() {
        return request == null ? "" : nvl(request.getToolArguments(), "");
    }

    public String getResponseContent() {
        return response == null ? "" : nvl(response.getContent(), "");
    }
}
