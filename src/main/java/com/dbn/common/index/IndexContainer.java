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

package com.dbn.common.index;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

@Slf4j
public class IndexContainer<T extends Indexable> {
    protected IndexCollection indices = new ArrayIndexCollection();

    public void add(T element) {
        indices.add(element.index());
    }

    public void add(int index) {
        indices.add(index);
    }

    public boolean addIfAbsent(T element) {
        return indices.addIfAbsent(element.index());
    }

    public boolean addIfAbsent(int index) {
        return indices.addIfAbsent(index);
    }

    public int size() {
        return indices.size();
    }

    public boolean isEmpty() {
        return indices.isEmpty();
    }

    public boolean contains(T indexable) {
        return indices.contains(indexable.index());
    }

    public final boolean contains(int index) {
        return indices.contains(index);
    }

    public Set<T> elements(IndexResolver<T> resolver) {
        if (indices.isEmpty()) return emptySet();

        return buildElements(resolver);
    }

    protected Set<T> buildElements(IndexResolver<T> resolver) {
        return buildElements(indices.values(), resolver);
    }

    protected Set<T> buildElements(int[] values, IndexResolver<T> resolver) {
        Set<T> elements = new LinkedHashSet<>(values.length, 0.75f);
        for (int value : values) {
            T element = resolver.apply(value);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }

    public void addAll(Collection<T> elements) {
        for (T element : elements) {
            indices.add(element.index());
        }
    }

    @FunctionalInterface
    public interface IndexResolver<R> {
        R apply(int index);
    }
}
