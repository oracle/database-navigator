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

package com.dbn.assistant.mcp;

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.approval.AssistantToolApprovalException;
import com.dbn.assistant.tool.event.AssistantToolEvent;
import com.dbn.assistant.tool.event.AssistantToolListener;
import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.execution.AssistantToolInvocationMonitor;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CancellationException;

import static com.dbn.assistant.tool.event.AssistantToolStatus.CANCELLED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.COMPLETED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.EXECUTING;
import static com.dbn.assistant.tool.event.AssistantToolStatus.FAILED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.REJECTED;
import static com.dbn.assistant.tool.event.AssistantToolStatus.REQUESTED;
import static com.dbn.common.action.UserDataKeys.ASSISTANT_MCP_SERVER_TOOL_INTERCEPTOR;
import static com.dbn.common.action.UserDataKeys.getUserDataSync;

@Slf4j
public class AssistantMcpServerToolInterceptor extends AssistantStateExtension {
    public AssistantMcpServerToolInterceptor(AssistantState assistantState) {
        super(assistantState);
    }


    public static AssistantMcpServerToolInterceptor get(AssistantState assistantState) {
        return getUserDataSync(assistantState, ASSISTANT_MCP_SERVER_TOOL_INTERCEPTOR,
                () -> new AssistantMcpServerToolInterceptor(assistantState));
    }

    @SneakyThrows
    public String invoke(ToolExecutor executor, ToolExecutionRequest request, Object memoryId) {
        AssistantToolInvocation invocation = AssistantToolInvocation.current();
        if (invocation == null) return null;

        //AssistantToolRequest request = invocation.getRequest();


        Project project = getProject();
        try {
            // initiate request
            handleEvent(project, invocation, REQUESTED, null);

            // wait for approval
            AssistantToolInvocationMonitor monitor = invocation.getMonitor();
            monitor.awaitApproval();

            // start execution
            handleEvent(project, invocation, EXECUTING, null);
            String result = monitor.executeTool(() -> executor.execute(request, memoryId));

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

    public static void handleEvent(Project project, AssistantToolInvocation invocation, AssistantToolStatus status, Throwable exception) {
        invocation.setStatus(status);
        AssistantToolRequest request = invocation.getRequest();
        AssistantToolEvent event = new AssistantToolEvent(request);
        event.setException(exception);
        ProjectEvents.notify(project, AssistantToolListener.TOPIC, l -> l.processEvent(event));
    }

}
