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

import com.dbn.object.DBColumn;
import com.dbn.object.properties.DBDataTypePresentableProperty;
import com.dbn.object.properties.DBObjectPresentableProperty;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;

import java.util.List;

public class DBColumnPropertiesProvider extends DBGenericObjectPropertiesProvider<DBColumn> {
    public DBColumnPropertiesProvider() {
        super(DBObjectType.COLUMN);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBColumn column) {
        List<DBObjectProperty> properties = super.getProperties(column);

        if (column.isForeignKey()) {
            DBColumn foreignKeyColumn = column.getForeignKeyColumn();
            if (foreignKeyColumn != null) {
                properties.add(0, new DBObjectPresentableProperty("Foreign key column", foreignKeyColumn, true));
            }
        }

        StringBuilder attributes  = new StringBuilder();
        if (column.isIdentity()) attributes.append("IDENTITY");
        if (column.isPrimaryKey()) attributes.append(" PK");
        if (column.isForeignKey()) attributes.append(" FK");
        if (!column.isPrimaryKey() && !column.isNullable()) attributes.append(" not null");

        if (!attributes.isEmpty()) {
            properties.add(0, new SimplePresentableProperty("Attributes", attributes.toString().trim()));
        }
        properties.add(0, new DBDataTypePresentableProperty(column.getDataType()));

        return properties;
    }
}
