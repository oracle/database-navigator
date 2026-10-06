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

import java.util.Set;

import static java.util.Collections.emptySet;

public final class BackedIndexContainer<T extends Indexable> extends IndexContainer<T> {
    private final IndexResolver<T> resolver;
    private volatile Set<T> elements;

    public BackedIndexContainer(IndexResolver<T> resolver, IndexCollection indices) {
        this.resolver = resolver;
        this.indices = indices;
    }

    public Set<T> elements() {
        Set<T> elements = this.elements;
        if (elements != null) return elements;

        synchronized (this) {
            elements = this.elements;
            if (elements == null) {
                elements = indices.isEmpty() ? emptySet() : buildElements(resolver);
                this.elements = elements;
            }
        }
        return elements;
    }

    public synchronized void freeze() {
        if (indices instanceof BitmapIndexCollection bitmap) {
            indices = bitmap.freeze();
        }
    }

}
