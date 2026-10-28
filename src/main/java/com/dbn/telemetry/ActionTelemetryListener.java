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

package com.dbn.telemetry;

import com.dbn.diagnostics.Diagnostics;
import com.dbn.telemetry.metrics.HistogramMetric;
import com.dbn.telemetry.metrics.MetricKey;
import com.dbn.telemetry.metrics.MetricType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.util.containers.CollectionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Records synchronous invocation durations for registered DBN actions. Work started asynchronously
 * after an action returns is outside this measurement. The histogram count also represents the
 * number of invocations observed by this listener.
 */
public final class ActionTelemetryListener implements AnActionListener {
    private static final String ACTION_ID_PREFIX = "DBN.";
    private static final String DURATION_METRIC_NAME = "dbn.action.duration";
    private static final String DURATION_UNIT = "s";
    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0);

    private final Map<AnActionEvent, ActionStart> actionStarts = CollectionFactory.createConcurrentWeakMap();

    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
        String actionId = ActionManager.getInstance().getId(action);
        if (actionId == null || !actionId.startsWith(ACTION_ID_PREFIX)) return;

        actionStarts.put(event, new ActionStart(actionId, System.nanoTime()));
    }

    @Override
    public void afterActionPerformed(
            @NotNull AnAction action,
            @NotNull AnActionEvent event,
            @NotNull AnActionResult result) {

        ActionStart start = actionStarts.remove(event);
        if (start == null) return;

        long elapsedNanos = System.nanoTime() - start.startedAtNanos();
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        try {
            MetricKey key = new MetricKey(
                    DURATION_METRIC_NAME,
                    MetricType.HISTOGRAM,
                    DURATION_UNIT,
                    Map.of("action_id", start.actionId()));
            TelemetryManager.getInstance().recordMetric(
                    HistogramMetric.observation(key, DURATION_BUCKETS_SECONDS, elapsedSeconds));
        } catch (RuntimeException exception) {
            // Telemetry collection must not interfere with IDE action execution.
            Diagnostics.conditionallyLog(exception);
        }
    }

    private record ActionStart(String actionId, long startedAtNanos) {}
}
