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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.util.containers.CollectionFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Records synchronous invocation durations for registered and dynamically constructed DBN actions. Work started asynchronously
 * after an action returns is outside this measurement. The histogram count also represents the
 * number of invocations observed by this listener.
 */
@NonNls
public final class ActionTelemetryListener extends TelemetryListener implements AnActionListener {
    private static final String ACTION_ID_PREFIX = "DBN.";
    private static final String DURATION_METRIC_NAME = "dbn.action.duration";
    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0);

    private final Map<AnActionEvent, ActionStart> actionStarts = CollectionFactory.createConcurrentWeakMap();

    @Override
    protected @NotNull String getMetricName() {
        return DURATION_METRIC_NAME;
    }

    @Override
    protected @NotNull List<Double> getDurationBucketsSeconds() {
        return DURATION_BUCKETS_SECONDS;
    }

    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
        if (!isCollectionEnabled()) return;

        String actionId = resolveActionId(action);
        if (actionId == null) return;

        long startedAtNanos = start();
        if (startedAtNanos != 0) actionStarts.put(event, new ActionStart(actionId, startedAtNanos));
    }

    /** Uses registered identifiers first, then the qualified class name for named DBN actions. */
    private static @Nullable String resolveActionId(@NotNull AnAction action) {
        String registeredId = ActionManager.getInstance().getId(action);
        if (registeredId != null) {
            return registeredId.startsWith(ACTION_ID_PREFIX) ? registeredId : null;
        }

        String className = action.getClass().getCanonicalName();
        if (className == null || !className.startsWith("com.dbn.")) return null;

        String name = className.substring("com.dbn.".length());
        if (name.endsWith("Action")) {
            name = name.substring(0, name.length() - "Action".length());
        }
        return "DBN.Action." + name;
    }

    @Override
    public void afterActionPerformed(
            @NotNull AnAction action,
            @NotNull AnActionEvent event,
            @NotNull AnActionResult result) {

        ActionStart start = actionStarts.remove(event);
        if (start == null) return;

        recordDuration(start.startedAtNanos(), () -> Map.of("action_id", start.actionId()));
    }

    private record ActionStart(String actionId, long startedAtNanos) {}
}
