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

package com.dbn.scheduler.model;

import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@Getter
public class SchedulerJobRequest {
    private final @NonNls String namePrefix;
    private final @NonNls String action;

    public SchedulerJobRequest(@NotNull @NonNls String namePrefix, @NotNull @NonNls String action) {
        if (namePrefix.isBlank()) throw new IllegalArgumentException("Job name prefix must not be blank");
        if (action.isBlank()) throw new IllegalArgumentException("Job action must not be blank");
        // the action must be fully-rendered text (a scheduler job runs later in its own session, so it
        // cannot carry JDBC binds). Build it from feature XML via DatabaseInterfaceBase.renderStatementText(...),
        // which rejects {#N} bind templates - do not concatenate SQL in Java.
        this.namePrefix = namePrefix;
        this.action = action;
    }
}
