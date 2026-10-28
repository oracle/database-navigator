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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A monotonic OpenTelemetry Sum, commonly used for invocation and error counts.
 */
@Getter
public final class CounterMetric implements Metric {
    private final MetricKey key;
    private final long sum;

    public CounterMetric(@NotNull MetricKey key, long sum) {
        this.key = key;
        if (key.type() != MetricType.SUM) {
            throw new IllegalArgumentException("Counter metrics require the SUM metric type");
        }
        if (sum < 0) throw new IllegalArgumentException("Counter sum must not be negative");
        this.sum = sum;
    }

    @JsonCreator
    public static CounterMetric fromJson(
            @JsonProperty("name") String name,
            @JsonProperty("unit") String unit,
            @JsonProperty("attributes") Map<String, String> attributes,
            @JsonProperty("sum") long sum) {
        return new CounterMetric(new MetricKey(name, MetricType.SUM, unit, attributes), sum);
    }

    public static @NotNull CounterMetric increment(@NotNull MetricKey key) {
        return increment(key, 1);
    }

    public static @NotNull CounterMetric increment(@NotNull MetricKey key, long delta) {
        if (delta < 0) throw new IllegalArgumentException("Counter increment must not be negative");
        return new CounterMetric(key, delta);
    }

    public boolean isMonotonic() {
        return true;
    }

    @Override
    public @NotNull CounterMetric merge(@NotNull Metric other) {
        if (!(other instanceof CounterMetric counterMetric) || !key.equals(counterMetric.key)) {
            throw new IllegalArgumentException("Cannot merge counters with different metric identities");
        }
        return new CounterMetric(key, Math.addExact(sum, counterMetric.sum));
    }

    @Override
    public @NotNull CounterMetric copy() {
        return new CounterMetric(key, sum);
    }
}
