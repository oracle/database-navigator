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

package com.dbn.migration.liquibase.execution;

import com.dbn.common.util.ExecutionTiming;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/** Shared lifecycle state for an item processed by a Liquibase operation. */
@Getter
public abstract class LiquibaseExecutionItem {
    public static final LiquibaseExecutionItemStatus DEFAULT_STATUS = LiquibaseExecutionItemStatus.PROCESSING;

    private LiquibaseExecutionItemStatus status;
    private String message;
    private final ExecutionTiming timing = new ExecutionTiming();

    protected LiquibaseExecutionItem(
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    public void updateStatus(
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    public void startProcessing() {
        timing.start();
    }

    public void finishProcessing() {
        timing.finish();
    }

    @NotNull
    public Duration getProcessingDuration() {
        return timing.getDuration();
    }
}
