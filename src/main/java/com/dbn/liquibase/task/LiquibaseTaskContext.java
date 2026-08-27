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

package com.dbn.liquibase.task;

import com.dbn.common.task.TaskStatus;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Per-run context shared by Liquibase operation and workflow tasks. */
@Getter
public abstract class LiquibaseTaskContext<I extends LiquibaseTaskInput> {
    private final I input;
    private volatile TaskStatus status = TaskStatus.NEW;
    private volatile boolean cancellationRequested;

    protected LiquibaseTaskContext(@NotNull I input) {
        this.input = input;
    }

    public Project getProject() {
        return input.getProject();
    }

    @NotNull
    public DatabaseLiquibaseManager getLiquibaseManager() {
        return DatabaseLiquibaseManager.getInstance(input.getProject());
    }

    public void cancel() {
        cancellationRequested = true;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested || Thread.currentThread().isInterrupted();
    }

    public void start() {
        status = TaskStatus.RUNNING;
    }

    public void pause() {
        status = TaskStatus.PAUSED;
    }

    public void finish(@NotNull TaskStatus status) {
        this.status = status;
    }
}
