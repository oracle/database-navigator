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

import com.dbn.code.common.intention.EditorIntentionActionEvent;
import com.dbn.code.common.intention.EditorIntentionActionListener;
import com.dbn.connection.ConnectionHandler;
import com.dbn.telemetry.ActionTelemetry;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.util.containers.CollectionFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.dbn.common.action.DataKeys.CONNECTION_HANDLER;
import static com.dbn.telemetry.ActionTelemetry.isTelemetryEnabled;
import static com.dbn.telemetry.ActionTelemetry.resolveActionId;

/**
 * Records synchronous invocation durations for registered and dynamically constructed DBN actions and editor intentions. Work started asynchronously
 * after an action returns is outside this measurement. The histogram count also represents the
 * number of invocations observed by this listener.
 */
@NonNls
public final class ActionTelemetryListener extends TelemetryListener implements AnActionListener, EditorIntentionActionListener {
    private static final String DURATION_METRIC_NAME = "dbn.action.duration";
    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0);

    private final Map<Object, ActionStart> actionStarts = CollectionFactory.createConcurrentWeakMap();

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
        if (!isCollectionEnabled() || !isTelemetryEnabled(action, event)) return;

        String actionId = resolveActionId(action);
        if (actionId == null) return;

        startInvocation(event, actionId, () -> event.getData(CONNECTION_HANDLER));
    }

    @Override
    public void beforeIntentionInvoked(@NotNull EditorIntentionActionEvent event) {
        startInvocation(event, event.getAction().getIntentionId(), () -> null);
    }

    @Override
    public void afterIntentionInvoked(@NotNull EditorIntentionActionEvent event) {
        recordInvocation(event);
    }

    private void startInvocation(
            @NotNull Object event,
            @NotNull String actionId,
            @NotNull Supplier<ConnectionHandler> connection) {
        Map<String, String> databaseInfo = databaseAttributes(connection);
        long startedAtNanos = start();
        if (startedAtNanos != 0) actionStarts.put(event, new ActionStart(actionId, startedAtNanos, databaseInfo));
    }

    @Override
    public void afterActionPerformed(
            @NotNull AnAction action,
            @NotNull AnActionEvent event,
            @NotNull AnActionResult result) {

        recordInvocation(event);
    }

    private void recordInvocation(@NotNull Object event) {
        ActionStart start = actionStarts.remove(event);
        if (start == null) return;

        recordDuration(start.startedAtNanos(), () -> {
            Map<String, String> attributes = new HashMap<>(start.databaseInfo());
            attributes.put("action_id", start.actionId());
            return attributes;
        });
    }

    private record ActionStart(String actionId, long startedAtNanos, Map<String, String> databaseInfo) {}
}
