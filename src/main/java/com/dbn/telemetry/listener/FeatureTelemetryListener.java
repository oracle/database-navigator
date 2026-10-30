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
import com.dbn.common.action.ActionFeatures;
import com.dbn.options.general.WorkspaceFeature;
import com.dbn.telemetry.TelemetryManager;
import com.dbn.telemetry.metrics.CounterMetric;
import com.dbn.telemetry.metrics.MetricKey;
import com.dbn.telemetry.metrics.MetricType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Counts entries through feature-tagged actions and intentions. Counts express usage, not successful completion
 * or distinct users; reports can be grouped by client ID to evaluate adoption.
 */
@NonNls
public final class FeatureTelemetryListener implements AnActionListener, EditorIntentionActionListener {
    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
        recordUsage(action.getClass());
    }

    @Override
    public void beforeIntentionInvoked(@NotNull EditorIntentionActionEvent event) {
        recordUsage(event.getAction().getClass());
    }

    private static void recordUsage(@NotNull Class<?> actionClass) {
        try {
            TelemetryManager manager = TelemetryManager.getInstance();
            if (!manager.isCollectionEnabled()) return;

            WorkspaceFeature feature = ActionFeatures.getFeature(actionClass);
            if (feature == null) return;

            MetricKey key = new MetricKey(
                    "dbn.feature.usage",
                    MetricType.SUM,
                    "{invocation}",
                    Map.of("feature", feature.name().toLowerCase(Locale.ROOT)));
            manager.recordMetric(CounterMetric.increment(key));
        } catch (RuntimeException exception) {
            // Optional telemetry must not prevent feature use.
            conditionallyLog(exception);
        }
    }
}
