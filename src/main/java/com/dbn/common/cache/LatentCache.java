/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbn.common.cache;

import com.dbn.common.thread.Background;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Weak-key cache whose values are loaded lazily in the background.
 *
 * A cache miss starts a load and returns {@code null} until the result is available.
 * When the stamp changes, the previous value is retained while the new value is
 * evaluated. Completion is reported through {@link #notify(Object, Object)}.
 */
public abstract class LatentCache<K, V> {
    private final Map<Object, CacheEntry<V>> cache = ContainerUtil.createConcurrentWeakMap();

    @Nullable
    public V get(@NotNull K key) {
        Object cacheKey = key(key);
        long stamp = stamp(key);
        CacheEntry<V> entry;
        synchronized (cache) {
            entry = cache.get(cacheKey);

            V value = entry == null ? null : entry.value;
            if (entry != null && entry.stamp == stamp) return value;

            entry = new CacheEntry<>(stamp, value);
            cache.put(cacheKey, entry);
        }

        CacheEntry<V> evaluation = entry;
        Background.run(() -> {
            V value = load(key);
            synchronized (cache) {
                if (cache.get(cacheKey) != evaluation) return;

                evaluation.value = value;
                evaluation.loaded = true;
            }
            notify(key, value);
        });
        return entry.value;
    }

    /**
     * Returns whether a load for the key's current stamp is still running.
     * Calling {@link #get(Object)} first ensures that the load has been scheduled.
     */
    public boolean isPending(@NotNull K key) {
        Object cacheKey = key(key);
        long stamp = stamp(key);
        synchronized (cache) {
            CacheEntry<V> entry = cache.get(cacheKey);
            return entry != null && entry.stamp == stamp && !entry.loaded;
        }
    }

    /**
     * Returns the stable identity used for storage. By default, the input key is used.
     */
    @NotNull
    protected Object key(@NotNull K key) {
        return key;
    }

    /**
     * Computes the value for a key in a background thread.
     */
    protected abstract V load(@NotNull K key);

    /**
     * Returns the key's current stamp. A changed stamp invalidates its cached value.
     */
    protected abstract long stamp(@NotNull K key);

    /**
     * Handles completion, including a {@code null} result. The implementation
     * chooses the appropriate thread or dispatch context.
     */
    protected abstract void notify(@NotNull K key, @Nullable V value);

    private static class CacheEntry<V> {
        private final long stamp;
        private volatile V value;
        private volatile boolean loaded;

        private CacheEntry(long stamp, @Nullable V value) {
            this.stamp = stamp;
            this.value = value;
        }
    }
}
