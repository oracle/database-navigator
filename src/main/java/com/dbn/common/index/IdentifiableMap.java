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

package com.dbn.common.index;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdentifiableMap<K, V extends Identifiable<K>>{
    private Map<K, V> data = new HashMap<>();

    public void rebuild(Collection<V> values) {
        rebuild(values.stream());
    }

    public void rebuild(Stream<V> values) {
        this.data = values.collect(Collectors.toMap(v -> v.getId(), v -> v));
    }

    public void clear() {
        data.clear();
    }

    public V get(K id) {
        return id == null ? null : data.get(id);
    }

    public boolean contains(K key) {
        return data.containsKey(key);
    }
}
