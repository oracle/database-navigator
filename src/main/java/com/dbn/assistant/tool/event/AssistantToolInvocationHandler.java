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

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.approval.AssistantToolApprovalException;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.execution.AssistantToolInvocationMonitor;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.assistant.tool.event.AssistantToolStatus.CANCELLED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.COMPLETED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.EXECUTING;
import static com.dbn.assistant.tool.event.AssistantToolStatus.FAILED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.REJECTED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.REQUESTED;
import static com.dbn.assistant.tool.execution.AssistantToolRequestVerifier.verifyRequest;

@Slf4j
public class AssistantToolInvocationHandler<T extends AssistantTool> extends AssistantStateExtension implements InvocationHandler {
    private final T tool;
    private static final Map<Method, Boolean> toolMethodCache = new ConcurrentHashMap<>();

    public AssistantToolInvocationHandler(AssistantState assistantState, T tool) {
        super(assistantState);
        this.tool = tool;
    }

    private static boolean isUtilityMethod(Method method) {
        return toolMethodCache.computeIfAbsent(method, m -> m.isAnnotationPresent(UtilitySpec.class));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!isUtilityMethod(method)) {
            return invokeMethod(method, args);
        }

        AssistantToolInvocation invocation = AssistantToolInvocation.current();
        if (invocation == null) return null;

        AssistantToolRequest request = invocation.getRequest();
        request.assertExecutable();
        request.setMethodArguments(args);
        verifyRequest(request, method, args);


        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();
        try {
            // initiate request
            handleEvent(project, invocation, REQUESTED, null);

            // wait for approval
            AssistantToolInvocationMonitor monitor = invocation.getMonitor();
            monitor.awaitApproval();

            // start execution
            handleEvent(project, invocation, EXECUTING, null);
            Object result;
            if (invocation.isInteractiveRequest()) {
                result = invocation.getOptionValue();
            } else {
                result = monitor.executeTool(() -> invokeMethod(method, args));
            }

            // confirm execution
            handleEvent(project, invocation, COMPLETED, null);
            return result;
        } catch (AssistantToolApprovalException e) {
            handleEvent(project, invocation, REJECTED, null);
            throw e;
        } catch (CancellationException e) {
            handleEvent(project, invocation, CANCELLED, e);
            throw new AssistantToolApprovalException("User has cancelled the execution of this tool", e);
        } catch (Throwable t) {
            Throwable exception = Exceptions.unwrap(t);
            handleEvent(project, invocation, FAILED, exception);
            throw exception;
        }
    }

    @SneakyThrows
    private Object invokeMethod(Method method, Object[] args) {
        try {
            return method.invoke(tool, args);
        } catch (Throwable e) {
            throw Exceptions.unwrap(e);
        }
    }

    public static void handleEvent(Project project, AssistantToolInvocation invocation, AssistantToolStatus status, Throwable exception) {
        invocation.setStatus(status);
        AssistantToolRequest request = invocation.getRequest();
        AssistantToolEvent event = new AssistantToolEvent(request);
        event.setException(exception);
        ProjectEvents.notify(project, AssistantToolListener.TOPIC, l -> l.processEvent(event));
    }
}
