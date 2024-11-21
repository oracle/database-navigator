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

package com.dbn.common.consumer;

import com.dbn.common.util.Commons;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SetCollector<T> implements Consumer<T> {
    private Set<T> elements;

    SetCollector() {}

    public static <T> SetCollector<T> basic() {
        return new SetCollector<>();
    }

    public static <T> SetCollector<T> concurrent() {
        return new SetCollector<>() {
            @Override
            protected Set<T> createSet() {
                return Collections.newSetFromMap(new ConcurrentHashMap<>());
            }
        };
    }

    public static <T> SetCollector<T> linked() {
        return new SetCollector<>() {
            @Override
            protected Set<T> createSet() {
                return new LinkedHashSet<>();
            }
        };
    }

    public static <T> SetCollector<T> sorted(Comparator<T> comparator) {
        return new SetCollector<>() {
            @Override
            protected Set<T> createSet() {
                return new TreeSet<>(comparator);
            }
        };
    }

    @Override
    public void accept(T element) {
        if (elements == null) {
            elements = createSet();
        }
        elements.add(element);
    }

    protected Set<T> createSet() {
        return new HashSet<>();
    }

    public Set<T> elements() {
        return Commons.nvl(elements, Collections.emptySet());
    }

    public boolean isEmpty() {
        return elements == null || elements.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public int size() {
        return elements == null ? 0 : elements.size();
    }

    public void clear() {
        elements.clear();
    }
}
