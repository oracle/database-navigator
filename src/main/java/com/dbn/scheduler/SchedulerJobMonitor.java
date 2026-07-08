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

package com.dbn.scheduler;

import com.intellij.openapi.util.NlsContexts.ProgressText;
import com.intellij.openapi.util.NlsContexts.ProgressTitle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Getter
public class SchedulerJobMonitor {
    private static final long DEFAULT_POLL_INTERVAL_MILLIS = 1_000;
    private static final long DEFAULT_TIMEOUT_MILLIS = 60 * 60 * 1_000;

    private final @ProgressTitle String title;
    private final @ProgressText String initialText;
    private final Function<SchedulerJobSnapshot, String> statusTextProvider;
    private final long pollIntervalMillis;
    private final long timeoutMillis;
    private final SchedulerJobCompletionPolicy completionPolicy;
    private final SchedulerJobCancellationPolicy cancellationPolicy;

    public SchedulerJobMonitor(
            @NotNull @ProgressTitle String title,
            @NotNull @ProgressText String initialText,
            @NotNull Function<SchedulerJobSnapshot, String> statusTextProvider) {
        this(title, initialText, statusTextProvider, DEFAULT_POLL_INTERVAL_MILLIS, DEFAULT_TIMEOUT_MILLIS,
                SchedulerJobCompletionPolicy.DROP, SchedulerJobCancellationPolicy.DETACH);
    }

    public SchedulerJobMonitor(
            @NotNull @ProgressTitle String title,
            @NotNull @ProgressText String initialText,
            @NotNull Function<SchedulerJobSnapshot, String> statusTextProvider,
            long pollIntervalMillis,
            long timeoutMillis,
            @NotNull SchedulerJobCompletionPolicy completionPolicy,
            @NotNull SchedulerJobCancellationPolicy cancellationPolicy) {
        if (pollIntervalMillis <= 0) throw new IllegalArgumentException("Poll interval must be positive");
        if (timeoutMillis <= 0) throw new IllegalArgumentException("Timeout must be positive");
        this.title = title;
        this.initialText = initialText;
        this.statusTextProvider = statusTextProvider;
        this.pollIntervalMillis = pollIntervalMillis;
        this.timeoutMillis = timeoutMillis;
        this.completionPolicy = completionPolicy;
        this.cancellationPolicy = cancellationPolicy;
    }
}
