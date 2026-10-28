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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Identifies one metric stream. Attribute values are intentionally strings and should be
 * bounded categories such as operation, database type, or outcome.
 */
public record MetricKey(
        @NotNull String name,
        @NotNull MetricType type,
        @NotNull String unit,
        @NotNull Map<String, String> attributes) {

    public MetricKey {
        if (name.isBlank()) throw new IllegalArgumentException("Metric name must not be blank");

        TreeMap<String, String> sortedAttributes = new TreeMap<>();
        attributes.forEach((attributeName, attributeValue) -> {
            if (attributeName == null || attributeName.isBlank()) {
                throw new IllegalArgumentException("Metric attribute name must not be blank");
            }
            sortedAttributes.put(attributeName, Objects.requireNonNull(attributeValue, "attributeValue"));
        });
        attributes = Collections.unmodifiableMap(sortedAttributes);
    }

    /**
     * Converts the bounded string dimensions to the OpenTelemetry API representation.
     */
    public @NotNull Attributes toOpenTelemetryAttributes() {
        AttributesBuilder builder = Attributes.builder();
        attributes.forEach((name, value) -> builder.put(AttributeKey.stringKey(name), value));
        return builder.build();
    }
}
