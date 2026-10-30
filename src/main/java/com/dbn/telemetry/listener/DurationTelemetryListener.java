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

import com.dbn.telemetry.TelemetryManager;
import com.dbn.telemetry.metrics.HistogramMetric;
import com.dbn.telemetry.metrics.MetricKey;
import com.dbn.telemetry.metrics.MetricType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/** Shared timing and histogram recording for duration telemetry collectors. */
public abstract class DurationTelemetryListener extends TelemetryListener {
    protected abstract @NotNull List<Double> getDurationBucketsSeconds();

    protected static long start() {
        return isCollectionEnabled() ? System.nanoTime() : 0;
    }

    protected final void recordDuration(
            long startedAtNanos,
            @NotNull Supplier<Map<String, String>> attributes) {
        if (startedAtNanos == 0) return;
        double durationSeconds = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0;
        try {
            TelemetryManager manager = TelemetryManager.getInstance();
            if (!manager.isCollectionEnabled()) return;

            MetricKey key = new MetricKey(getTelemetryType().getMetricName(), MetricType.HISTOGRAM, "s", attributes.get());
            manager.recordMetric(HistogramMetric.observation(key, getDurationBucketsSeconds(), durationSeconds));
        } catch (RuntimeException exception) {
            // Telemetry must not interfere with the operation being measured.
            conditionallyLog(exception);
        }
    }
}
