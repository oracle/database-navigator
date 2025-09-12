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

package com.dbn.assistant.tool.event;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.approval.AssistantToolExecutionMonitor;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.lang.reflect.Method;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Commons.nvl;

@Getter
@Setter
public class AssistantToolRequest implements PersistentStateElement {
    private static final ThreadLocal<AssistantToolRequest> CURRENT = new ThreadLocal<>();
    private String chatId;
    private String requestId;
    private String toolName;
    private String toolArguments;

    private AssistantTool tool;

    private Method method;
    private Object[] arguments;

    private AssistantToolStatus status = AssistantToolStatus.REQUESTED;
    private AssistantToolExecutionMonitor executionMonitor;

    public AssistantToolRequest() {}

    public AssistantToolRequest(AssistantToolCache toolCache, String chatId, String requestId, String toolName, String toolArguments) {
        this.chatId = chatId;
        this.requestId = nvl(requestId, () -> UUIDs.compact());
        this.toolName = toolName;
        this.toolArguments = toolArguments;

        this.tool = toolCache.getAssistantTool(toolName);
        this.method = AssistantToolCache.getUtilityMethod(tool, toolName);

        CURRENT.set(this);
    }

    public void verify(Method method) {
        if (!this.method.equals(method)) {
            throw new IllegalArgumentException("The method to verify does not match the current request");
        }
    }

    public static AssistantToolRequest current() {
        // NOTE: this assumes the tool concurrency is disabled
        // TODO find alternative ways to propagate this context to the java.lang.invoke.DelegatingMethodHandle
        return CURRENT.get();
    }

    public void readState(Element element) {
        requestId = stringAttribute(element, "request-id");
        toolName = stringAttribute(element, "tool-name");
        status = enumAttribute(element, "tool-status", status);

        Element contentElement = element.getChild("tool-arguments");
        toolArguments = readCdata(contentElement);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "request-id", requestId);
        setStringAttribute(element, "tool-name", toolName);
        setEnumAttribute(element, "tool-status", status);

        Element contentElement = newElement(element,"tool-arguments");
        writeCdata(contentElement, toolArguments);
    }
}
