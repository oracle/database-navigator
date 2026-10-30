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

package com.dbn.telemetry.listener;

import com.dbn.common.outcome.OutcomeType;
import com.dbn.execution.ExecutionType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Measures the execution engine's cancellable call, excluding prompts, queueing and later result fetching.
 * Cancellation duration measures the cancellation handler, not confirmation that database work has stopped.
 */
public final class ExecutionTelemetryListener extends TelemetryListener {

    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0, 600.0);
    private static final CancellationTelemetryListener CANCELLATION_LISTENER = new CancellationTelemetryListener();

    private static final Map<ExecutionType, ExecutionTelemetryListener> LISTENERS = new EnumMap<>(ExecutionType.class);

    static {
        for (ExecutionType executionType : ExecutionType.values()) {
            LISTENERS.put(executionType, new ExecutionTelemetryListener(executionType));
        }
    }

    private final ExecutionType executionType;

    private ExecutionTelemetryListener(@NotNull ExecutionType executionType) {
        this.executionType = executionType;
    }

    public static @NotNull ExecutionTelemetryListener getInstance(@NotNull ExecutionType executionType) {
        return LISTENERS.get(executionType);
    }

    @Override
    protected @NotNull String getMetricName() {
        return "dbn.execution.duration";
    }

    @Override
    protected @NotNull List<Double> getDurationBucketsSeconds() {
        return DURATION_BUCKETS_SECONDS;
    }

    void recordExecution(long startedAtNanos, @NotNull OutcomeType outcome, @NotNull Map<String, String> databaseAttributes) {
        recordDuration(startedAtNanos, () -> attributes(outcome, databaseAttributes));
    }

    void recordCancellation(long startedAtNanos, @NotNull OutcomeType outcome, @NotNull Map<String, String> databaseAttributes) {
        CANCELLATION_LISTENER.recordDuration(startedAtNanos, () -> attributes(outcome, databaseAttributes));
    }

    private Map<String, String> attributes(OutcomeType outcome, Map<String, String> databaseAttributes) {
        Map<String, String> attributes = new HashMap<>(databaseAttributes);
        attributes.put("execution_type", executionType.name().toLowerCase(Locale.ROOT));
        attributes.put("outcome", outcome.name().toLowerCase(Locale.ROOT));
        return attributes;
    }

    private static final class CancellationTelemetryListener extends TelemetryListener {
        @Override
        protected @NotNull String getMetricName() {
            return "dbn.execution.cancellation.duration";
        }

        @Override
        protected @NotNull List<Double> getDurationBucketsSeconds() {
            return DURATION_BUCKETS_SECONDS;
        }
    }
}
