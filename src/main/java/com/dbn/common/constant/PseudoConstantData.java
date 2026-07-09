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

package com.dbn.common.constant;

import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class PseudoConstantData<T extends PseudoConstant<T>> {
    static final ThreadLocal<PseudoConstantData> LOCAL = new ThreadLocal<>();

    private final Class<T> type;
    private final Map<String, T> mappings = new ConcurrentHashMap<>();
    private final List<T> values = new ArrayList<>();
    private final Lock lock = new ReentrantLock();

    public PseudoConstantData(Class<T> type) {
        this.type = type;
        init();
    }

    private void init() {
        try {
            lock.lock();
            LOCAL.set(this);
            createConstant(null);
        } finally {
            LOCAL.remove();
            lock.unlock();
        }
    }

    T get(String id) {
        T constant = mappings.get(id);
        if (constant != null) return constant;

        // The constructor registers the new instance. Do not hold the registry
        // lock while invoking it: class initialization and constructor code may
        // request another pseudo constant and create a lock cycle.
        createConstant(id);

        // Another thread may have won the race and registered the canonical
        // instance while this instance was being constructed.
        return mappings.get(id);
    }

    int register(T constant) {
        try {
            lock.lock();
            String id = constant.id();

            T registered = mappings.get(id);
            if (registered != null) return registered.ordinal();

            int ordinal = mappings.size();
            ensureCapacity(ordinal);
            values.set(ordinal, constant);
            mappings.put(id, constant);
            return ordinal;
        } finally {
            lock.unlock();
        }
    }

    private void ensureCapacity(int index) {
        while (values.size() <= index) {
            values.add(null);
        }
    }

    @SneakyThrows
    private T createConstant(String id) {
        Constructor<T> constructor = type.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(id);
    }

    public int size() {
        return values.size();
    }

    public Collection<T> values() {
        return values;
    }
}
