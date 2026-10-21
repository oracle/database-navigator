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

package com.dbn.common.resource;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable limits for one operation that consumes input-controlled resources.
 *
 * <p>Limits are named and scoped to an operation because safe values depend on
 * the resource being guarded. Callers should verify a value before parsing,
 * allocating, retaining, or iterating over the corresponding resource.</p>
 */
public final class ResourceLimits {
    private final Map<String, ResourceLimit> limits;

    private ResourceLimits(Map<String, ResourceLimit> limits) {
        this.limits = Collections.unmodifiableMap(new LinkedHashMap<>(limits));
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public ResourceLimit get(@NotNull @NonNls String name) {
        ResourceLimit limit = limits.get(name);
        if (limit == null) {
            throw new IllegalArgumentException("No resource limit is defined for " + name);
        }
        return limit;
    }

    public boolean allows(@NotNull @NonNls String name, long value) {
        return get(name).allows(value);
    }

    public long verify(@NotNull String name, long value) {
        return get(name).verify(value);
    }

    public Map<String, ResourceLimit> asMap() {
        return limits;
    }

    /**
     * Counts UTF-8 bytes without allocating an encoded copy, stopping once the count exceeds {@code maximum}.
     * The result is exact when it is within the maximum; otherwise it is a lower bound.
     * Malformed surrogates count as the single replacement byte used by Java's UTF-8 encoder.
     */
    public static long getUtf8ByteCount(@NotNull String value, long maximum) {
        if (maximum < 0) {
            throw new IllegalArgumentException("Maximum byte count must not be negative");
        }

        long byteCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character <= 0x7F) {
                byteCount++;
            } else if (character <= 0x7FF) {
                byteCount += 2;
            } else if (Character.isHighSurrogate(character) && i + 1 < value.length() &&
                    Character.isLowSurrogate(value.charAt(i + 1))) {
                byteCount += 4;
                i++;
            } else if (Character.isSurrogate(character)) {
                byteCount++;
            } else {
                byteCount += 3;
            }
            if (byteCount > maximum) return byteCount;
        }
        return byteCount;
    }

    public static boolean isWithinRange(long value, long minimum, long maximum) {
        verifyRange(minimum, maximum);
        return value >= minimum && value <= maximum;
    }

    public static long clampToRange(long value, long minimum, long maximum) {
        verifyRange(minimum, maximum);
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static void verifyRange(long minimum, long maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum value must not exceed maximum value");
        }
    }

    public static final class Builder {
        private final Map<String, ResourceLimit> limits = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder add(@NotNull @NonNls String name, long maximum) {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Resource limit name must not be blank");
            }
            if (maximum < 0) {
                throw new IllegalArgumentException("Resource limit must not be negative: " + name);
            }
            ResourceLimit previous = limits.putIfAbsent(name, new ResourceLimit(name, maximum));
            if (previous != null) {
                throw new IllegalArgumentException("Resource limit is already defined: " + name);
            }
            return this;
        }

        public ResourceLimits build() {
            return new ResourceLimits(limits);
        }
    }

}
