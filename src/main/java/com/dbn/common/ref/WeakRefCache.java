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

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface WeakRefCache<K, V> {
    V get(K key);

    V ensure(K key);

    V get(K key, Function<K, V> loader);

    V compute(K key, BiFunction<K, V, V> loader);

    V computeIfAbsent(K key, Function<? super K, ? extends V> loader);

    void set(K key, V value);

    V remove(K key);

    boolean contains(K key);

    Set<K> keys();

    void clear();

    static <K, V> WeakRefCache<K, V> weakKey() {
        return new WeakRefCacheKeyImpl<>();
    }

    static <K, V> WeakRefCache<K, V> weakKeyValue() {
        return new WeakRefCacheKeyValueImpl<>();
    }
}
