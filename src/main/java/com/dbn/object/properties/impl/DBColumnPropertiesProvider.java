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
import java.util.StringJoiner;

import static com.dbn.nls.NlsResources.txt;

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
                properties.add(0, new DBObjectPresentableProperty(txt("app.objects.property.ForeignKeyColumn"), foreignKeyColumn, true));
            }
        }

        StringJoiner attributes = new StringJoiner(" ");
        if (column.isIdentity()) attributes.add(txt("app.objects.propertyValue.Identity"));
        if (column.isPrimaryKey()) attributes.add(txt("app.objects.propertyValue.PrimaryKeyShort"));
        if (column.isForeignKey()) attributes.add(txt("app.objects.propertyValue.ForeignKeyShort"));
        if (!column.isPrimaryKey() && !column.isNullable()) attributes.add(txt("app.objects.propertyValue.NotNull"));

        if (attributes.length() > 0) {
            properties.add(0, new SimplePresentableProperty(txt("app.objects.property.Attributes"), attributes.toString()));
        }
        properties.add(0, new DBDataTypePresentableProperty(column.getDataType()));

        return properties;
    }
}
