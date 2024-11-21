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
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;

import java.util.List;

@Getter
class DBObjectNavigationListImpl<T extends DBObject> implements DBObjectNavigationList<T> {
    private final String name;
    private final DBObjectRef<T> object;
    private final List<DBObjectRef<T>> objects;
    private ObjectListProvider<T> objectsProvider;

    DBObjectNavigationListImpl(String name, T object) {
        this.name = name;
        this.object = DBObjectRef.of(object);
        this.objects = null;
    }

    DBObjectNavigationListImpl(String name, List<T> objects) {
        this.name = name;
        this.object = null;
        this.objects = DBObjectRef.from(objects);
    }

    DBObjectNavigationListImpl(String name, ObjectListProvider<T> objectsProvider) {
        this.name = name;
        this.object = null;
        this.objects = null;
        this.objectsProvider = objectsProvider;
    }

    @Override
    public T getObject() {
        return DBObjectRef.get(object);
    }

    @Override
    public List<T> getObjects() {
        return objects == null ? null : DBObjectRef.get(objects);
    }

    @Override
    public boolean isLazy() {
        return objectsProvider != null;
    }
}
