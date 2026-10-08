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

package com.dbn.common.cache;

import java.util.Arrays;

/**
 * Immutable composite key backed by an array of attributes.
 */
public final class ObjectKey {
    private final Object[] attributes;

    private ObjectKey(Object... attributes) {
        this.attributes = Arrays.copyOf(attributes, attributes.length);
    }

    public static ObjectKey create(Object... attributes) {
        return new ObjectKey(attributes);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ObjectKey other)) return false;
        return Arrays.deepEquals(attributes, other.attributes);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(attributes);
    }
}
