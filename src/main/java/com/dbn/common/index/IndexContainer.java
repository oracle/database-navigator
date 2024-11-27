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

import com.dbn.common.util.Compactable;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class IndexContainer<T extends Indexable> implements Compactable {
    private final IndexCollection INDEX = new IndexCollection();

    public void add(T element) {
        INDEX.add(element.index());
    }

    public boolean isEmpty() {
        return INDEX.isEmpty();
    }

    public boolean contains(T indexable) {
        return INDEX.contains(indexable.index());
    }

    public Set<T> elements(IndexResolver<T> resolver) {
        if (INDEX.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<T> elements = new HashSet<>(INDEX.size());
            int[] values = INDEX.values();
            for (int value : values) {
                T element = resolver.apply(value);
                if (element != null) {
                    elements.add(element);
                }
            }
            return elements;
        }
    }

    @Override
    public void compact() {
        //INDEX.trimToSize();
    }

    public void addAll(Collection<T> elements) {
        for (T element : elements) {
            INDEX.add(element.index());
        }
    }


    @FunctionalInterface
    public interface IndexResolver<R> {
        R apply(int index);
    }
}
