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


import com.intellij.util.containers.IntObjectMap;

import static com.intellij.concurrency.ConcurrentCollectionFactory.createConcurrentIntObjectMap;

public class IndexRegistry<T extends Indexable> {
    private final IntObjectMap<T> INDEX = createConcurrentIntObjectMap();

    public void add(T element) {
        INDEX.put(element.index(), element);
    }

    public T get(int index) {
        return INDEX.get(index);
    }

    public int size() {
        return INDEX.size();
    }
}
