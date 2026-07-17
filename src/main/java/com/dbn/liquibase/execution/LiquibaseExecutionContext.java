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

package com.dbn.liquibase.execution;

import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Per-run state shared by a Liquibase processor and its execution result. */
@Getter
public class LiquibaseExecutionContext {
    private final LiquibaseExecutionInput input;
    private LiquibaseExecutionResult result;
    private volatile Thread executionThread;
    private volatile boolean cancellationRequested;

    public LiquibaseExecutionContext(@NotNull LiquibaseExecutionInput input) {
        this.input = input;
    }

    public Project getProject() {
        return input.getProject();
    }

    public @NotNull DatabaseLiquibaseManager getLiquibaseManager() {
        return DatabaseLiquibaseManager.getInstance(input.getProject());
    }

    @NotNull
    public LiquibaseExecutionResult prepareExecutionResult() {
        if (result == null) result = new LiquibaseExecutionResult(input);
        return result;
    }

    public void cancel() {
        cancellationRequested = true;
        Thread thread = executionThread;
        if (thread != null) thread.interrupt();
    }

    public boolean isCancellationRequested() {
        return cancellationRequested || Thread.currentThread().isInterrupted();
    }

    @NotNull
    public DBSchema getSourceSchema() {
        DBSchema schema = input.getSourceSchema();
        if (schema == null) throw new IllegalStateException("Source schema not specified");
        return schema;
    }

    @NotNull
    public DBSchema getTargetSchema() {
        DBSchema schema = input.getTargetSchema();
        if (schema == null) throw new IllegalStateException("Target schema not specified");
        return schema;
    }

    void setExecutionThread(@NotNull Thread executionThread) {
        this.executionThread = executionThread;
    }

    void clearExecutionThread() {
        executionThread = null;
    }
}
