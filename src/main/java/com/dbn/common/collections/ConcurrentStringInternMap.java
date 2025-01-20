/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.common.collections;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A thread-safe class that provides string interning functionality while storing and retrieving
 * key-value pairs. Uses {@link ConcurrentHashMap} as the underlying storage mechanism, ensuring
 * high concurrency in read and write operations. Both keys and values are automatically interned
 * when added or accessed, reducing memory usage by reusing identical string instances.
 */
public class ConcurrentStringInternMap {
    private final Map<String, String> data = new ConcurrentHashMap<>();

    public String put(@NotNull String key, @NotNull String value) {
        return data.put(key.intern(), value.intern());
    }

    public String get(String key) {
        return data.get(key.intern());
    }

    public String computeIfAbsent(String key, Function<String, String> mapper) {
        return data.computeIfAbsent(key.intern(), k -> mapper.apply(k).intern());
    }

    public void clear() {
        data.clear();
    }
}
