/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.execution.statement;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.dbn.common.dispose.Failsafe.guarded;

public final class StatementExecutionQueue extends StatefulDisposableBase {
    private final Queue<StatementExecutionProcessor> processors = new ConcurrentLinkedQueue<>();
    private final ConnectionRef connection;
    private volatile boolean executing = false;

    public StatementExecutionQueue(ConnectionHandler connection) {
        super(connection);
        this.connection = connection.ref();
    }

    void queue(StatementExecutionProcessor processor) {
        StatementExecutionContext context = processor.getExecutionContext();
        context.set(ExecutionStatus.CANCELLED, false);
        if (!this.processors.contains(processor)) {
            context.set(ExecutionStatus.QUEUED, true);
            this.processors.offer(processor);
            execute();
        }
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    private synchronized void execute() {
        if (executing) return;
        executing = true;

        Project project = getProject();
        ConnectionHandler connection = getConnection();
        Progress.background(project, connection, true,
                "Executing statements",
                "Executing SQL statements",
                progress -> {
                    try {
                        StatementExecutionProcessor processor = processors.poll();
                        while (processor != null) {
                            execute(processor);

                            if (progress.isCanceled()) {
                                cancelExecution();
                            }
                            processor = processors.poll();
                        }
                    } finally {
                        executing = false;
                        if (progress.isCanceled()) {
                            cancelExecution();
                        }
                    }
                });
    }

    private void execute(StatementExecutionProcessor processor) {
        guarded(processor, p -> {
            Project project = p.getProject();
            StatementExecutionContext context = p.getExecutionContext();
            context.set(ExecutionStatus.QUEUED, false);
            context.set(ExecutionStatus.EXECUTING, true);
            StatementExecutionManager statementExecutionManager = StatementExecutionManager.getInstance(project);
            statementExecutionManager.process(p);
        });
    }

    private void cancelExecution() {
        // cleanup queue for untouched processors
        StatementExecutionProcessor processor = processors.poll();
        while(processor != null) {
            processor.getExecutionContext().reset();
            processor = processors.poll();
        }
    }

    public boolean contains(StatementExecutionProcessor processor) {
        return processors.contains(processor);
    }

    public void cancelExecution(StatementExecutionProcessor processor) {
        processor.getExecutionContext().set(ExecutionStatus.QUEUED, false);
        processors.remove(processor);
        if (processors.isEmpty()) executing = false;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
