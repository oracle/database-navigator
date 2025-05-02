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

package com.dbn.framework.task;

import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeHandlers;
import com.dbn.common.outcome.OutcomeHandlersImpl;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.Getter;

import java.util.LinkedList;
import java.util.Queue;

import static com.dbn.common.load.ProgressMonitor.isProgressCancelled;

public class TaskQueue {
    private final Queue<Task> tasks = new LinkedList<>();
    private final OutcomeHandlers outcomeHandlers = new OutcomeHandlersImpl();

    @Getter
    private boolean cancelled = false;

    public void push(Object subject, Runnable runnable) {
        tasks.add(new Task(subject, runnable));
    }

    public final void addOutcomeHandler(OutcomeType outcomeType, OutcomeHandler handler) {
        if (handler == null) return;
        outcomeHandlers.addHandler(outcomeType, handler);
    }

    public void execute() {
        while (!tasks.isEmpty()) {
            Task task = tasks.poll();
            Object subject = task.getSubject();

            try {
                if (cancelled || isProgressCancelled()) {
                    cancelled = true;
                    return;
                }

                task.getRunnable().run();
                Outcome outcome = Outcome.
                        success().
                        withMessage("Task completed successfully").
                        withData(subject);

                outcomeHandlers.handle(outcome);
            } catch (ProcessCanceledException e) {
                Diagnostics.conditionallyLog(e);
            } catch (Exception e) {

                Outcome outcome = Outcome.
                        failure().
                        withMessage("Task failed").
                        withData(subject).
                        withException(e);

                outcomeHandlers.handle(outcome);
            }
        }
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isComplete() {
        return tasks.isEmpty();
    }
}
