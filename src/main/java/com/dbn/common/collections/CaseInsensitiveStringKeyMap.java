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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Strings.toLowerCase;

public class CaseInsensitiveStringKeyMap<V> implements Map<String, V> {
    private final Map<String, V> data = new HashMap<>();

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(internalKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return data.get(internalKey(key));
    }

    @Nullable
    @Override
    public V put(String key, V value) {
        return data.put(internalKey(key), value);
    }

    @Override
    public V remove(Object key) {
        return data.remove(internalKey(key));
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends V> m) {
        m.forEach((k, v) -> data.put(internalKey(k), v));
    }

    @Override
    public void clear() {
        data.clear();
        entrySet().clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return data.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, V>> entrySet() {
        return data.entrySet();
    }

    private static String internalKey(Object key) {
        return key == null ? "" : toLowerCase(key.toString());
    }
}
