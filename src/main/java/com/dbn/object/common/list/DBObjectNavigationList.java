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

import com.dbn.object.common.DBObject;

import java.util.List;

public interface DBObjectNavigationList<T extends DBObject> {
    DBObjectNavigationList[] EMPTY_ARRAY = new DBObjectNavigationList[0];

    String getName();
    T getObject();
    List<T> getObjects();
    ObjectListProvider<T> getObjectsProvider();
    boolean isLazy();


    static <T extends DBObject> DBObjectNavigationList<T> create(String name, T object) {
        return new DBObjectNavigationListImpl<>(name, object);
    }

    static <T extends DBObject> DBObjectNavigationList<T> create(String name, List<T> objects) {
        return new DBObjectNavigationListImpl<>(name, objects);
    }

    static <T extends DBObject> DBObjectNavigationList<T> create(String name, ObjectListProvider<T> objectsProvider) {
        return new DBObjectNavigationListImpl<>(name, objectsProvider);
    }
}
