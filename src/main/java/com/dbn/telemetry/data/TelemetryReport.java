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

package com.dbn.telemetry.data;

import com.dbn.telemetry.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON report envelope with flexible version metadata and strongly typed metric items. */
@NoArgsConstructor
public final class TelemetryReport {
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private Metrics metrics;

    public TelemetryReport(Map<String, Object> metadata, List<Metric> items) {
        this.metadata.putAll(metadata);
        this.metrics = new Metrics(List.copyOf(items));
    }

    @JsonAnyGetter
    public Map<String, Object> metadata() {
        return metadata;
    }

    @JsonAnySetter
    public void putMetadata(String name, Object value) {
        metadata.put(name, value);
    }

    @JsonProperty("metrics")
    public Metrics metrics() {
        return metrics;
    }

    @JsonProperty("metrics")
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public String batchId() {
        return (String) metadata.get("batch_id");
    }

    public record Metrics(List<Metric> items) {
    }
}
