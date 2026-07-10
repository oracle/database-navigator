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
import org.jetbrains.annotations.Nullable;

@Getter
public class SchedulerJobSnapshot {
    private final String state;
    private final String runStatus;
    private final String errorNumber;
    private final String additionalInfo;
    private final SchedulerJobStatus status;

    public SchedulerJobSnapshot(
            @Nullable String state,
            @Nullable String runStatus,
            @Nullable String errorNumber,
            @Nullable String additionalInfo) {
        this.state = state;
        this.runStatus = runStatus;
        this.errorNumber = errorNumber;
        this.additionalInfo = additionalInfo;
        this.status = SchedulerJobStatus.resolve(state, runStatus);
    }

    public static SchedulerJobSnapshot notFound() {
        return new SchedulerJobSnapshot(null, null, null, null);
    }
}
