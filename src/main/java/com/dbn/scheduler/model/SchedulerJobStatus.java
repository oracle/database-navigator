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

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum SchedulerJobStatus {
    SCHEDULED,
    RUNNING,
    COMPLETED,
    SUCCEEDED,
    FAILED,
    BROKEN,
    STOPPED,
    UNKNOWN;

    public boolean isTerminal() {
        return isOneOf(SUCCEEDED, FAILED, BROKEN, STOPPED);
    }

    public boolean isSuccessful() {
        return this == SUCCEEDED;
    }

    public boolean isOneOf(SchedulerJobStatus... statuses) {
        for (SchedulerJobStatus status : statuses) {
            if (this == status) return true;
        }
        return false;
    }

    public static SchedulerJobStatus resolve(@Nullable String state, @Nullable String runStatus) {
        SchedulerJobStatus status = parse(runStatus);
        return status == UNKNOWN ? parse(state) : status;
    }

    private static SchedulerJobStatus parse(@Nullable String value) {
        try {
            return value == null ? UNKNOWN : SchedulerJobStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignore) {
            return UNKNOWN;
        }
    }
}
