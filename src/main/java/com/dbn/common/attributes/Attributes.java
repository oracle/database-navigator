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

package com.dbn.common.attributes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class Attributes {
    private final Map<AttributeKey<?>, Object> values = new HashMap<>();

    private Attributes() {
    }

    public static Attributes create() {
        return new Attributes();
    }

    public static Attributes empty() {
        return new Attributes();
    }

    public static Attributes of(@Nullable Attributes attr) {
        return attr == null ? empty() : attr;
    }

    public <T> Attributes set(AttributeKey<T> key, @Nullable T value) {
        if (value != null && !key.getType().isInstance(value)) {
            throw new IllegalArgumentException("Invalid value type for attribute '" + key.id() + "'");
        }

        values.put(key, value);
        return this;
    }

    @Nullable
    public <T> T get(AttributeKey<T> key) {
        Object value = values.get(key);
        if (value == null) return null;
        Class<T> type = key.getType();
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public <T> T get(AttributeKey<T> key, T defaultValue) {
        T value = get(key);
        return value == null ? defaultValue : value;
    }

    @NotNull
    public <T> T require(AttributeKey<T> key) {
        T value = get(key);
        if (value != null) return value;

        throw new IllegalArgumentException("Missing attribute '" + key.id() + "'");
    }

    public boolean contains(AttributeKey<?> key) {
        return values.containsKey(key);
    }

    public Attributes remove(AttributeKey<?> key) {
        values.remove(key);
        return this;
    }
}
