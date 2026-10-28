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
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Keeps complete collection windows together, including pending and in-flight batches. */
public final class TelemetryBatchCache implements PersistentStateElement {
    private static final long BATCH_AGE_MILLIS = Duration.ofHours(5).toMillis();
    private final List<TelemetryBatch> batches = new ArrayList<>();

    public synchronized void record(@NotNull Metric metric, long now, @NotNull Supplier<Map<String, Object>> metadata) {
        TelemetryBatch active = batches.isEmpty() ? null : batches.get(batches.size() - 1);
        if (active != null && !active.isClosed() && now - active.getStartedAt() >= BATCH_AGE_MILLIS) {
            active.close(now);
        }
        if (active == null || active.isClosed()) {
            active = new TelemetryBatch(now, metadata.get());
            batches.add(active);
        }
        active.record(metric);
    }

    public synchronized List<TelemetryReport> pendingReports() {
        return batches.stream().filter(batch -> batch.isClosed()).map(TelemetryBatch::report).toList();
    }

    public synchronized void closeActiveBatch(long endedAt) {
        if (batches.isEmpty()) return;
        TelemetryBatch active = batches.get(batches.size() - 1);
        if (!active.isClosed()) active.close(endedAt);
    }

    public synchronized boolean hasPendingReports() {
        return batches.stream().anyMatch(batch -> batch.isClosed());
    }

    public synchronized void acknowledge(String batchId) {
        batches.removeIf(batch -> batch.isClosed() && batchId.equals(batch.getBatchId()));
    }

    public synchronized List<Metric> snapshot() {
        return batches.stream().flatMap(batch -> batch.snapshot().stream()).toList();
    }

    @Override
    public synchronized void writeState(Element state) {
        for (TelemetryBatch batch : batches) {
            batch.writeState(state, "batch");
        }
    }

    @Override
    public synchronized void readState(Element state) {
        List<TelemetryBatch> restored = new ArrayList<>();
        for (Element element : state.getChildren("batch")) {
            TelemetryBatch batch = new TelemetryBatch();
            batch.readState(element);
            restored.add(batch);
        }
        batches.clear();
        batches.addAll(restored);
    }
}
