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

package com.dbn.common.ref;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.dbn.common.dispose.Failsafe.nd;

abstract class WeakRefCacheBase<K, V> implements WeakRefCache<K, V> {
    private final Map<K, V> cache = createCache();

    protected abstract Map<K, V> createCache();

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public V ensure(K key) {
        return nd(get(key));
    }

    @Override
    @SneakyThrows
    public V get(K key, Function<K, V> loader) {
        return cache.computeIfAbsent(key, loader);
    }

    @Override
    public V compute(K key, BiFunction<K, V, V> loader) {
        return cache.compute(key, loader);
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> loader) {
        return cache.computeIfAbsent(key, loader);
    }

    @Override
    public void set(K key, @Nullable V value) {
        if (value == null)
            cache.remove(key);
        else
            cache.put(key, value);
    }

    @Override
    public V remove(K key) {
        return cache.remove(key);
    }

    @Override
    public boolean contains(K key) {
        return cache.containsKey(key);
    }

    @Override
    public Set<K> keys() {
        return cache.keySet();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
