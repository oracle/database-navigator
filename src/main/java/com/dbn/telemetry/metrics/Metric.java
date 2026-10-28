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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Immutable aggregate for one metric stream. Implementations define their own merge rules.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CounterMetric.class, name = "sum"),
        @JsonSubTypes.Type(value = HistogramMetric.class, name = "histogram")
})
@JsonIgnoreProperties(value = {"temporality", "monotonic"}, allowGetters = true)
public interface Metric {
    @JsonIgnore
    @NotNull MetricKey getKey();

    /**
     * Returns a new aggregate containing this metric and {@code other}.
     */
    @NotNull Metric merge(@NotNull Metric other);

    /**
     * Returns an independent immutable copy suitable for a report snapshot.
     */
    @NotNull Metric copy();

    default @NotNull String getName() {
        return getKey().name();
    }

    default @NotNull String getType() {
        return getKey().type().getOpenTelemetryName();
    }

    default @NotNull String getUnit() {
        return getKey().unit();
    }

    default @NotNull Map<String, String> getAttributes() {
        return getKey().attributes();
    }

    /**
     * DBN reports non-overlapping session aggregates, so supported metric points use delta
     * temporality. The report envelope supplies the start and end timestamps.
     */
    default @NotNull String getTemporality() {
        return "delta";
    }
}
