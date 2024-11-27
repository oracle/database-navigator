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

import com.dbn.object.DBOrderedObject;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

abstract class DBObjectComparatorBase<T extends DBObject> implements Comparator<T> {
    public static int compareRef(@Nullable DBObject o1, @Nullable DBObject o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }
        return o1.ref().compareTo(o2.ref());
    }

    public static int compareType(DBObject o1, DBObject o2) {
        DBObjectType type1 = o1.getObjectType();
        DBObjectType type2 = o2.getObjectType();
        return type1.compareTo(type2);
    }

    public static int compareName(DBObject o1, DBObject o2) {
        String name1 = o1.getName();
        String name2 = o2.getName();
        return name1.compareToIgnoreCase(name2);
    }

    public static int compareOverload(DBObject o1, DBObject o2) {
        short overload1 = o1.getOverload();
        short overload2 = o2.getOverload();
        return Short.compare(overload1, overload2);
    }

    public static int compareProperty(DBObject o1, DBObject o2, DBObjectProperty property) {
        boolean property1 = o1.is(property);
        boolean property2 = o2.is(property);
        return Boolean.compare(property1, property2);
    }

    public static int comparePosition(DBOrderedObject o1, DBOrderedObject o2) {
        short position1 = o1.getPosition();
        short position2 = o2.getPosition();
        return Short.compare(position1, position2);
    }
}
