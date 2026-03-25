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

package com.dbn.object.properties.impl;

import com.dbn.object.common.DBObject;
import com.dbn.object.properties.ConnectionPresentableProperty;
import com.dbn.object.properties.DBObjectPresentableProperty;
import com.dbn.object.properties.DBObjectPropertiesProvider;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DBGenericObjectPropertiesProvider<T extends DBObject> implements DBObjectPropertiesProvider<T> {
    private final DBObjectType objectType;

    public DBGenericObjectPropertiesProvider(DBObjectType objectType) {
        this.objectType = objectType;
    }

    public DBGenericObjectPropertiesProvider() {
        this(DBObjectType.ANY);
    }

    @Override
    public List<DBObjectProperty> getProperties(T object) {
        List<DBObjectProperty> properties = new ArrayList<>();
        DBObject parent = object.getParentObject();
        while (parent != null) {
            properties.add(new DBObjectPresentableProperty(parent));
            parent = parent.getParentObject();
        }
        properties.add(new ConnectionPresentableProperty(object.getConnection()));

        return properties;
    }
}
