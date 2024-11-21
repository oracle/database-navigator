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

package com.dbn.object.common.sorting;

import com.dbn.common.latent.Latent;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.type.DBObjectType;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Unsafe.cast;

@UtilityClass
public class DBObjectComparators {
    private static final Map<Key, DBObjectComparator<?>> entries = new ConcurrentHashMap<>();
    private static final Latent<DBObjectComparator> generic = Latent.basic(() -> new DBObjectComparator.Generic());
    private static final Latent<DBObjectComparator> classic = Latent.basic(() -> new DBObjectComparator.Classic());

    static {
        register(new DBColumnNameComparator());
        register(new DBColumnPositionComparator());
        register(new DBProcedureNameComparator());
        register(new DBProcedurePositionComparator());
        register(new DBFunctionNameComparator());
        register(new DBFunctionPositionComparator());
        register(new DBArgumentNameComparator());
        register(new DBArgumentPositionComparator());
        register(new DBTypeAttributeNameComparator());
        register(new DBTypeAttributePositionComparator());
    }

    private static void register(DBObjectComparator comparator) {
        Key key = Key.of(comparator.getObjectType(), comparator.getSortingType());
        entries.put(key, comparator);
    }

    public static <T extends DBObject> DBObjectComparator<T> basic(DBObjectType objectType) {
        Key key = Key.of(objectType, null, SortingType.NAME, true);
        return cast(entries.computeIfAbsent(key, k -> new DBObjectComparator.Basic(k.getObjectType())));
    }

    @Nullable
    public static <T extends DBObject> DBObjectComparator<T> predefined(DBObjectType objectType, SortingType sortingType) {
        Key key = Key.of(objectType, sortingType);
        return cast(entries.get(key));
    }

    public static <T extends DBObject> DBObjectComparator<T> classic() {
        return cast(classic.get());
    }

    public static <T extends DBObject> DBObjectComparator<T> generic() {
        return cast(generic.get());
    }

    public static <T extends DBObject> DBObjectComparator<T> detailed(DBObjectType objectType, DBObjectProperty property, SortingType sortingType) {
        Key key = Key.of(objectType, property, sortingType, false);
        return cast(entries.computeIfAbsent(key, k -> new DBObjectComparator.Detailed(property)));
    }

    @Value
    static class Key {
        private final DBObjectType objectType;
        private final DBObjectProperty property;
        private final SortingType sortingType;
        private final boolean virtual;

        public static Key of(DBObjectType objectType, SortingType sortingType) {
            return of(objectType, null, sortingType, false);
        }

        public static Key of(DBObjectType objectType, DBObjectProperty objectProperty, SortingType sortingType, boolean virtual) {
            return new Key(objectType, objectProperty, sortingType, virtual);
        }
    }
}
