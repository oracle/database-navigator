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

package com.dbn.execution;

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.property.PropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.debugger.DBDebuggerType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.execution.ExecutionStatus.CANCELLED;
import static com.dbn.execution.ExecutionStatus.EXECUTING;
import static com.dbn.execution.ExecutionStatus.QUEUED;
import static com.dbn.execution.ExecutionStatus.VALUES;

@Getter
@Setter
public abstract class ExecutionContext<T extends ExecutionInput> extends PropertyHolderBase.IntStore<ExecutionStatus> implements PropertyHolder<ExecutionStatus> {
    private int timeout;
    private boolean logging = false;
    private long executionTimestamp;
    private DBNConnection connection;
    private DBNStatement statement;
    private DBDebuggerType debuggerType;
    private WeakRef<T> input;
    private WeakRef<ProgressIndicator> progress;

    public ExecutionContext(T input) {
        this.input = WeakRef.of(input);
        this.progress = WeakRef.of(ProgressMonitor.getProgressIndicator());
    }

    @NotNull
    public T getInput() {
        return WeakRef.ensure(input);
    }

    @Override
    protected ExecutionStatus[] properties() {
        return VALUES;
    }

    @NotNull
    public abstract String getTargetName();

    @Nullable
    public abstract ConnectionHandler getTargetConnection();

    @Nullable
    public abstract SchemaId getTargetSchema();

    public boolean canExecute() {
        return isNot(QUEUED) && isNot(EXECUTING) && isNot(CANCELLED);
    }

    public <S extends DBNStatement> S getStatement() {
        return cast(statement);
    }

    public Project getProject() {
        return getInput().getProject();
    }

    @Nullable
    public ProgressIndicator getProgress() {
        return WeakRef.get(progress);
    }

    @Override
    public void reset() {
        super.reset();
        timeout = 0;
        logging = false;
        executionTimestamp = System.currentTimeMillis();
        connection = null;
        statement = null;
        this.progress = WeakRef.of(ProgressMonitor.getProgressIndicator());
    }
}
