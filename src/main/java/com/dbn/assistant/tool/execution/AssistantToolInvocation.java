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

import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.assistant.tool.AssistantToolData.isInteractiveTool;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;

@Getter
@Setter
public class AssistantToolInvocation implements PersistentStateElement {
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
        return isInteractiveTool(request.getUtilityName());
    }

    public static AssistantToolInvocation current() {
        // NOTE: this assumes the tool concurrency is disabled
        // TODO find alternative ways to propagate this context to the com.dbn.assistant.tool.event.AssistantToolInvocationHandler
        return CURRENT.get();
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
        request.setUtilityName(stringAttribute(element, "tool-name"));
        option = stringAttribute(element, "tool-option");
        status = enumAttribute(element, "tool-status", status);

        Element argumentsElement = element.getChild("tool-arguments");
        request.setUtilityArguments(readCdata(argumentsElement));

        Element responseElement = element.getChild("tool-response");
        if (responseElement != null) {
            String toolResponse = readCdata(responseElement);
            response = new AssistantToolResponse(toolResponse);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "request-id", request.getRequestId());
        setStringAttribute(element, "tool-name", request.getUtilityName());
        setStringAttribute(element, "tool-option", option);
        setEnumAttribute(element, "tool-status", status);

        Element contentElement = newElement(element,"tool-arguments");
        writeCdata(contentElement, request.getUtilityArguments());

        if (response != null) {
            Element resposeElement = newElement(element,"tool-response");
            writeCdata(resposeElement, response.getContent());
        }
    }
}
