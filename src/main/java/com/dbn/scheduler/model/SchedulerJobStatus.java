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

import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum SchedulerJobStatus implements Presentable {
    SCHEDULED(txt("app.scheduler.const.SchedulerJobStatus_SCHEDULED")),
    RUNNING(txt("app.scheduler.const.SchedulerJobStatus_RUNNING")),
    COMPLETED(txt("app.scheduler.const.SchedulerJobStatus_COMPLETED")),
    SUCCEEDED(txt("app.scheduler.const.SchedulerJobStatus_SUCCEEDED")),
    FAILED(txt("app.scheduler.const.SchedulerJobStatus_FAILED")),
    BROKEN(txt("app.scheduler.const.SchedulerJobStatus_BROKEN")),
    STOPPED(txt("app.scheduler.const.SchedulerJobStatus_STOPPED")),
    UNKNOWN(txt("app.scheduler.const.SchedulerJobStatus_UNKNOWN"));

    private final String name;

    SchedulerJobStatus(String name) {
        this.name = name;
    }

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
