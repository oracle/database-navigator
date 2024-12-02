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

package com.dbn.object.common.list;

import com.dbn.common.content.DynamicContentType;
import com.dbn.connection.DatabaseType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class DynamicContentTypeIndex<O extends DynamicContentType, T extends DynamicContentType> {

    private final Class<O> ownerType;
    private final Map<DatabaseType, Entry<T>[]> entries = new EnumMap<>(DatabaseType.class);

    public DynamicContentTypeIndex(Class<O> ownerType) {
        this.ownerType = ownerType;
    }

    public synchronized int index(DatabaseType databaseType, O ownerType, T type) {
        Entry<T> entry = entry(databaseType, ownerType, (Class<T>) type.getClass());
        return entry.index(type);
    }

    private Entry<T> entry(DatabaseType databaseType, O ownerType, Class<T> type) {
        int position = position(ownerType);
        Entry<T>[] entries = entries(databaseType);
        if (entries[position] == null) {
            entries[position] = new Entry<>(type);
        }
        return entries[position];
    }

    private Entry<T>[] entries(DatabaseType databaseType) {
        Entry<T>[] entries = this.entries.get(databaseType);
        if (entries == null) {
            entries = new Entry[ownerType.getEnumConstants().length];
            this.entries.put(databaseType, entries);
        }
        return entries;
    }

    private static class Entry<T extends DynamicContentType> {
        private final Class<T> type;
        private final int[] indexes;
        private int size = 0;

        Entry(Class<T> type) {
            this.type = type;
            this.indexes = new int[type.getEnumConstants().length];
            Arrays.fill(indexes, -1);
        }

        int index(T type) {
            int position = position(type);
            if (indexes[position] == -1) {
                indexes[position] = size;
                size++;
            }
            return indexes[position];
        }

        @Override
        public String toString() {
            return type.getSimpleName() + " " + size;
        }
    }

    public String toString() {
        return ownerType.getSimpleName();
    }


    private static int position(DynamicContentType type) {
        Enum e = (Enum) type;
        return e.ordinal();
    }
}
