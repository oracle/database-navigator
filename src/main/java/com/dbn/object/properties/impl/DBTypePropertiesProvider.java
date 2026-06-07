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

import com.dbn.data.type.DBDataType;
import com.dbn.object.DBType;
import com.dbn.object.properties.DBDataTypePresentableProperty;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.type.DBObjectType;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DBTypePropertiesProvider extends DBGenericObjectPropertiesProvider<DBType> {
    public DBTypePropertiesProvider() {
        super(DBObjectType.TYPE);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBType type) {
        List<DBObjectProperty> properties = super.getProperties(type);
        DBDataType collectionElementType = type.getCollectionElementType();
        if (collectionElementType != null) {
            properties.add(0, new DBDataTypePresentableProperty(txt("app.objects.property.CollectionElementType"), collectionElementType));
        }
        return properties;
    }
}
