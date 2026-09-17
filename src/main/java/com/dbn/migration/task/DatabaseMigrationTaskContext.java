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

package com.dbn.migration.task;

import com.dbn.common.task.TaskStatus;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Common per-run state for a database migration task.
 *
 * <p>Status and cancellation are deliberately engine-neutral. Cancellation is cooperative:
 * engine-specific execution code must check {@link #isCancellationRequested()} at safe points
 * and translate the request into its own connection or API cancellation mechanism.</p>
 */
@Getter
public abstract class DatabaseMigrationTaskContext<I extends DatabaseMigrationTaskInput> {
    private final I input;
    private volatile TaskStatus status = TaskStatus.NEW;
    private volatile boolean cancellationRequested;

    protected DatabaseMigrationTaskContext(@NotNull I input) {
        this.input = input;
    }

    @NotNull
    public Project getProject() {
        return input.getProject();
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
