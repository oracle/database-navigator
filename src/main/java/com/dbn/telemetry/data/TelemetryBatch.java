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

import com.dbn.common.state.PersistentStateElement;
import com.dbn.telemetry.metrics.Metric;
import com.dbn.telemetry.metrics.MetricCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.longAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setLongAttribute;

/** A complete collection window, retained until its report has been accepted. */
@Getter
@Setter
@NoArgsConstructor
public final class TelemetryBatch implements PersistentStateElement {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private long startedAt;
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private MetricCollector metrics = new MetricCollector();
    private boolean closed;

    public TelemetryBatch(long startedAt, Map<String, Object> metadata) {
        this.startedAt = startedAt;
        this.metadata.putAll(metadata);
    }

    public String getBatchId() {
        return (String) metadata.get("batch_id");
    }

    public void close(long endedAt) {
        closed = true;
        metadata.put("period_end", Instant.ofEpochMilli(endedAt).toString());
    }

    public void record(Metric metric) {
        metrics.record(metric);
    }

    public List<Metric> snapshot() {
        return metrics.snapshot();
    }

    public TelemetryReport report() {
        return new TelemetryReport(metadata, metrics.snapshot());
    }

    @Override
    @SneakyThrows
    public void writeState(Element element) {
        setLongAttribute(element, "started-at", startedAt);
        setBooleanAttribute(element, "closed", closed);
        element.setText(OBJECT_MAPPER.writeValueAsString(report()));
    }

    @Override
    @SneakyThrows
    public void readState(Element element) {
        long restoredStart = longAttribute(element, "started-at", startedAt);
        boolean restoredClosed = booleanAttribute(element, "closed", closed);
        TelemetryReport report = OBJECT_MAPPER.readValue(element.getText(), TelemetryReport.class);
        MetricCollector restoredMetrics = new MetricCollector();
        restoredMetrics.recordAll(report.metrics().items());
        startedAt = restoredStart;
        closed = restoredClosed;
        metadata.clear();
        metadata.putAll(report.metadata());
        metrics = restoredMetrics;
    }

}
