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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Specialised concurrent HashMap
 * Supports nullable values while inheriting the optimised synchronisation logic of {@link ConcurrentHashMap}
 * Wraps values with {@link Optional} 
 *
 * @param <K>
 * @param <V>
 */
public class ConcurrentOptionalValueMap<K, V> implements Map<K, V> {

    private final Map<K, Optional<V>> inner;

    public ConcurrentOptionalValueMap() {
        inner = new ConcurrentHashMap<>();
    }

    public ConcurrentOptionalValueMap(int initialCapacity) {
        this.inner = new ConcurrentHashMap<>(initialCapacity);
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) return false;
        return inner.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return inner.containsValue(wrap(value));
    }

    @Override
    public V get(Object key) {
        if (key == null) return null;
        return unwrap(inner.get(key));
    }

    @Override
    public V put(K key, V value) {
        if (key == null) return null;
        return unwrap(inner.put(key, wrap(value)));
    }

    @Override
    public V remove(Object key) {
        if (key == null) return null;
        return unwrap(inner.remove(key));
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mapper) {
        if (key == null) return null;
        return unwrap(inner.computeIfAbsent(key, k -> wrap(mapper.apply(k))));
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remapper) {
        if (key == null) return null;
        return unwrap(inner.computeIfPresent(key, (k, v) -> wrap(remapper.apply(k, unwrap(v)))));
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remapper) {
        if (key == null) return null;
        return unwrap(inner.compute(key, (k, v) -> wrap(remapper.apply(k, unwrap(v)))));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach((key, value) -> put(key, value));
    }

    @Override
    public void clear() {
        inner.clear();
    }

    @Override
    public Set<K> keySet() {
        return inner.keySet();
    }

    @Override
    public Collection<V> values() {
        return inner.values().stream().map(v -> unwrap(v)).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return inner.entrySet().stream().map(e -> new Entry<K, V>() {
            @Override
            public K getKey() {
                return e.getKey();
            }

            @Override
            public V getValue() {
                return unwrap(e.getValue());
            }

            @Override
            public V setValue(V value) {
                return value;
            }
        }).collect(Collectors.toSet());
    }

    private V unwrap(Optional<V> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    private Optional<V> wrap(Object value) {
        return (Optional<V>) Optional.ofNullable(value);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
