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

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import java.util.ArrayList;
import java.util.List;

/**
 * An OpenTelemetry Histogram aggregate. The average is derived from {@code sum / count};
 * bucket counts preserve a mergeable distribution for later percentile estimates.
 */
@Getter
public final class HistogramMetric implements Metric {
    private final MetricKey key;
    private final List<Double> bucketBoundaries;
    private final long count;
    private final double sum;
    private final double min;
    private final double max;
    private final List<Long> bucketCounts;

    public HistogramMetric(
            @NotNull MetricKey key,
            @NotNull List<Double> bucketBoundaries,
            long count,
            double sum,
            double min,
            double max,
            @NotNull List<Long> bucketCounts) {

        this.key = key;
        if (key.type() != MetricType.HISTOGRAM) {
            throw new IllegalArgumentException("Histogram metrics require the HISTOGRAM metric type");
        }
        this.bucketBoundaries = validateBoundaries(bucketBoundaries);
        if (count <= 0) throw new IllegalArgumentException("Histogram count must be positive");
        if (!Double.isFinite(sum) || !Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Histogram values must be finite and min must not exceed max");
        }
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
        this.bucketCounts = validateBucketCounts(bucketCounts, this.bucketBoundaries.size(), count);
    }

    @JsonCreator
    public static HistogramMetric fromJson(
            @JsonProperty("name") String name,
            @JsonProperty("unit") String unit,
            @JsonProperty("attributes") Map<String, String> attributes,
            @JsonProperty("bucketBoundaries") List<Double> bucketBoundaries,
            @JsonProperty("count") long count,
            @JsonProperty("sum") double sum,
            @JsonProperty("min") double min,
            @JsonProperty("max") double max,
            @JsonProperty("bucketCounts") List<Long> bucketCounts) {
        return new HistogramMetric(new MetricKey(name, MetricType.HISTOGRAM, unit, attributes),
                bucketBoundaries, count, sum, min, max, bucketCounts);
    }

    public static @NotNull HistogramMetric observation(
            @NotNull MetricKey key,
            @NotNull List<Double> bucketBoundaries,
            double value) {

        if (!Double.isFinite(value)) throw new IllegalArgumentException("Histogram observation must be finite");
        List<Double> boundaries = validateBoundaries(bucketBoundaries);
        List<Long> counts = new ArrayList<>(boundaries.size() + 1);
        int bucketIndex = 0;
        while (bucketIndex < boundaries.size() && value > boundaries.get(bucketIndex)) {
            bucketIndex++;
        }
        for (int i = 0; i <= boundaries.size(); i++) {
            counts.add(i == bucketIndex ? 1L : 0L);
        }
        return new HistogramMetric(key, boundaries, 1, value, value, value, counts);
    }

    public double average() {
        return sum / count;
    }

    @Override
    public @NotNull HistogramMetric merge(@NotNull Metric other) {
        if (!(other instanceof HistogramMetric histogramMetric) ||
                !key.equals(histogramMetric.key) ||
                !bucketBoundaries.equals(histogramMetric.bucketBoundaries)) {
            throw new IllegalArgumentException("Cannot merge histograms with different metric identities or buckets");
        }

        List<Long> mergedBucketCounts = new ArrayList<>(bucketCounts.size());
        for (int i = 0; i < bucketCounts.size(); i++) {
            mergedBucketCounts.add(Math.addExact(bucketCounts.get(i), histogramMetric.bucketCounts.get(i)));
        }
        double mergedSum = sum + histogramMetric.sum;
        if (!Double.isFinite(mergedSum)) throw new ArithmeticException("Histogram sum overflow");

        return new HistogramMetric(
                key,
                bucketBoundaries,
                Math.addExact(count, histogramMetric.count),
                mergedSum,
                Math.min(min, histogramMetric.min),
                Math.max(max, histogramMetric.max),
                mergedBucketCounts);
    }

    @Override
    public @NotNull HistogramMetric copy() {
        return new HistogramMetric(key, bucketBoundaries, count, sum, min, max, bucketCounts);
    }

    private static @NotNull List<Double> validateBoundaries(@NotNull List<Double> boundaries) {
        List<Double> copy = List.copyOf(boundaries);
        double previous = Double.NEGATIVE_INFINITY;
        for (Double boundary : copy) {
            if (!Double.isFinite(boundary) || boundary <= previous) {
                throw new IllegalArgumentException("Histogram bucket boundaries must be finite and strictly increasing");
            }
            previous = boundary;
        }
        return copy;
    }

    private static @NotNull List<Long> validateBucketCounts(
            @NotNull List<Long> counts,
            int boundaryCount,
            long expectedCount) {

        if (counts.size() != boundaryCount + 1) {
            throw new IllegalArgumentException("Histogram requires one bucket count per boundary plus the +Inf bucket");
        }
        List<Long> copy = List.copyOf(counts);
        long actualCount = 0;
        for (Long bucketCount : copy) {
            if (bucketCount < 0) throw new IllegalArgumentException("Histogram bucket counts must not be negative");
            actualCount = Math.addExact(actualCount, bucketCount);
        }
        if (actualCount != expectedCount) {
            throw new IllegalArgumentException("Histogram bucket counts must add up to the observation count");
        }
        return copy;
    }
}
