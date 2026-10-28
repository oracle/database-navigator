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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe in-memory container for incremental telemetry aggregates.
 * Metrics with the same identity are merged by their own {@link Metric#merge(Metric)} logic.
 */
public final class MetricCollector {
    private final ConcurrentHashMap<MetricKey, Metric> metrics = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void record(@NotNull Metric metric) {
        lock.readLock().lock();
        try {
            MetricKey key = metric.getKey();
            metrics.compute(key, (ignored, current) -> {
                if (current == null) return metric.copy();
                Metric merged = current.merge(metric);
                if (!key.equals(merged.getKey())) {
                    throw new IllegalStateException("Merged metric changed its identity");
                }
                return merged;
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    public void recordAll(@NotNull Collection<? extends Metric> recordedMetrics) {
        for (Metric metric : recordedMetrics) record(metric);
    }

    /**
     * Returns an immutable, consistent snapshot without resetting accumulated values.
     */
    public @NotNull List<Metric> snapshot() {
        lock.writeLock().lock();
        try {
            return snapshotMetrics();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Atomically returns the current aggregate and starts an empty collection window.
     * The returned metrics remain available to the caller for retry if submission fails.
     */
    public @NotNull List<Metric> drain() {
        lock.writeLock().lock();
        try {
            List<Metric> snapshot = snapshotMetrics();
            metrics.clear();
            return snapshot;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return metrics.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    private @NotNull List<Metric> snapshotMetrics() {
        List<Metric> snapshot = new ArrayList<>(metrics.size());
        metrics.values().forEach(metric -> snapshot.add(metric.copy()));
        snapshot.sort(Comparator
                .comparing((Metric metric) -> metric.getKey().name())
                .thenComparing(metric -> metric.getKey().type())
                .thenComparing(metric -> metric.getKey().unit())
                .thenComparing(metric -> metric.getKey().attributes().toString()));
        return List.copyOf(snapshot);
    }
}
