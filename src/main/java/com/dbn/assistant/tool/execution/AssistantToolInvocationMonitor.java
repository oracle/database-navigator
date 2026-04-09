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

import com.dbn.assistant.mcp.AssistantMcpServer;
import com.dbn.assistant.mcp.AssistantMcpServerData;
import com.dbn.assistant.mcp.AssistantMcpToolApprovals;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.approval.AssistantToolApprovalException;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.common.EntityId;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.Threads;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.thread.ThreadProperty.BACKGROUND;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.concurrent.TimeUnit.MINUTES;

public class AssistantToolInvocationMonitor extends AssistantStateExtension {
    private final AssistantTool tool;
    private final String toolName;
    private CountDownLatch approvalLatch;
    private boolean approved;
    private boolean cancelled;
    private Future<?> promise;

    public AssistantToolInvocationMonitor(@NotNull AssistantState assistantState, @NotNull AssistantTool tool,  @NotNull String toolName) {
        super(assistantState);
        this.toolName = toolName;
        this.tool = tool;
    }

    public void awaitApproval() {
        if (tool.isExternal()) {
            awaitMcpToolApproval();
        } else {
            awaitToolApproval();
        }
    }

    private void awaitMcpToolApproval() {
        AssistantState state = getAssistantState();
        AssistantMcpToolApprovals approvals = state.getMcpToolApprovals();

        AssistantMcpServerData mcpServerData = state.getMcpServerData();
        AssistantMcpServer mcpServer = mcpServerData.resolveMcpServer(toolName);
        if (mcpServer == null) throw new AssistantToolApprovalException("Can't resolve mcp server for tool name \"" + toolName + "\"");

        EntityId serverId = mcpServer.getId();
        String utilityName = mcpServer.unqualifiedUtilityName(toolName);

        if (approvals.isApproved(serverId, utilityName)) return;
        if (approvals.isBlocked(serverId, utilityName)) throw new AssistantToolApprovalException("User has denied the execution of this tool");
        if (approvals.isBlocked(serverId)) throw new AssistantToolApprovalException("User has denied the execution of this MCP server");

        awaitApproval(1, MINUTES); // TODO configuration
    }

    private void awaitToolApproval() {
        AssistantState state = getAssistantState();
        AssistantToolApprovals approvals = state.getToolApprovals();
        if (approvals.isApproved(tool)) return;

        if (approvals.isBlocked(tool.getType())) throw new AssistantToolApprovalException("User has denied the execution of this tool type");
        if (approvals.isBlocked(tool.getCategory())) throw new AssistantToolApprovalException("User has denied the execution of this tool category");

        awaitApproval(1, MINUTES); // TODO configuration
    }

    private void awaitApproval(long timeout, TimeUnit unit) {
        try {
            approvalLatch = new CountDownLatch(1);

            boolean inTime = approvalLatch.await(timeout, unit);
            if (!inTime) throw new AssistantToolApprovalException("User has not approved in time. Tool execution approval has timed out");

            if (cancelled) throw new AssistantToolApprovalException("User has cancelled the execution of this tool");
            if (!approved) throw new AssistantToolApprovalException("User has denied the execution of this tool");
        } catch (AssistantToolApprovalException e) {
            throw e;
        } catch (Exception e) {
            throw Exceptions.toRuntimeException(e);
        }
    }

    @SneakyThrows
    public <T> T executeTool(ThrowableCallable<T, Exception> callable) {
        ThreadInfo invoker = ThreadInfo.copy();
        try {
            ExecutorService executorService = Threads.assistantToolExecutor();
            promise = executorService.submit(() -> ThreadMonitor.surround(invoker, BACKGROUND, callable));
            return cast(promise.get(1, MINUTES)); // TODO tool timeout configuration
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.unwrap(e);
        }
    }

    public void allow() {
        approved = true;
        releaseLatch();
    }

    public void deny() {
        releaseLatch();
    }

    public void cancel() {
        cancelled = true;
        cancelPromise();
        releaseLatch();
    }

    private void cancelPromise() {
        Future promise = this.promise;
        if (promise == null) return;

        promise.cancel(true);
    }

    private void releaseLatch() {
        CountDownLatch approvalLatch = this.approvalLatch;
        if (approvalLatch == null) return;

        approvalLatch.countDown();
    }
}
