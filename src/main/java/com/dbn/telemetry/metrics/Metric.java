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

package com.dbn.telemetry.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Immutable aggregate for one metric stream. Implementations define their own merge rules.
 */
public interface Metric {
    @NotNull MetricKey key();

    /**
     * Returns a new aggregate containing this metric and {@code other}.
     */
    @NotNull Metric merge(@NotNull Metric other);

    /**
     * Returns an independent immutable copy suitable for a report snapshot.
     */
    @NotNull Metric copy();

    default @NotNull String getName() {
        return key().name();
    }

    default @NotNull String getType() {
        return key().type().getOpenTelemetryName();
    }

    default @NotNull String getUnit() {
        return key().unit();
    }

    default @NotNull Map<String, String> getAttributes() {
        return key().attributes();
    }

    /**
     * DBN reports non-overlapping daily aggregates, so supported metric points use delta
     * temporality. The report envelope supplies the start and end timestamps.
     */
    default @NotNull String getTemporality() {
        return "delta";
    }
}
