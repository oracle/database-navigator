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

package com.dbn.assistant.tool.approval;

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantTool;
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
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.concurrent.TimeUnit.MINUTES;

public class AssistantToolExecutionMonitor extends AssistantStateExtension {
    private final AssistantTool tool;
    private CountDownLatch approvalLatch;
    private boolean approved;
    private Future promise;

    public AssistantToolExecutionMonitor(@NotNull AssistantState assistantState, AssistantTool tool) {
        super(assistantState);
        this.tool = tool;
    }

    public void awaitApproval() {
        AssistantState state = getAssistantState();
        AssistantToolApprovals approval = AssistantToolApprovals.get(state);
        if (approval.isAllowed(tool.getCategory())) return;
        if (approval.isAllowed(tool.getType())) return;

        if (approval.isDenied(tool.getType())) throw new AssistantToolApprovalException("User has denied the execution of this tool type");
        if (approval.isDenied(tool.getCategory())) throw new AssistantToolApprovalException("User has denied the execution of this tool category");

        awaitApproval(1, MINUTES); // TODO configuration
    }

    private void awaitApproval(long timeout, TimeUnit unit) {
        try {
            approvalLatch = new CountDownLatch(1);

            boolean inTime = approvalLatch.await(timeout, unit);
            if (!inTime) throw new AssistantToolApprovalException("User has not approved in time. Tool execution approval has timed out");
            //if (canceled) throw new AssistantToolApprovalException("User has cancelled the execution of this tool");
            if (!approved) throw new AssistantToolApprovalException("User has denied the execution of this tool");
        } catch (AssistantToolApprovalException e) {
            throw e;
        } catch (Exception e) {
            throw Exceptions.toRuntimeException(e);
        }
    }

    @SneakyThrows
    public Object executeTool(ThrowableCallable<?, Exception> callable) {
        ThreadInfo invoker = ThreadInfo.copy();
        try {
            ExecutorService executorService = Threads.assistantToolExecutor();
            promise = executorService.submit(() -> ThreadMonitor.surround(invoker, BACKGROUND, callable));
            return promise.get(1, MINUTES); // TODO tool timeout configuration
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.unwrap(e);
        }
    }

    public void allow() {
        approved = true;
        approvalLatch.countDown();
    }

    public void deny() {
        approvalLatch.countDown();
    }

    public void cancel() {
        if (promise == null) return;
        promise.cancel(true);
    }

}
