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

/**
 * Collection of primitive indexes used by parser caches.
 *
 * Implementations may use different storage strategies depending on the density of the indexes.
 */
public interface IndexCollection {
    int[] EMPTY_ARRAY = new int[0];

    boolean isEmpty();

    boolean contains(int value);

    int indexOf(int value);

    int size();

    int[] values();

    void add(int value);

    boolean addIfAbsent(int value);
}
