/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.factory.model;

import com.dbn.common.util.Strings;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class DBTableFactoryInput extends DBSchemaObjectFactoryInput {
    private DBObjectFactoryInputList<DBColumnFactoryInput> columns = new DBObjectFactoryInputList<>(this);

    public DBTableFactoryInput(DBSchema schema) {
        super(schema, DBObjectType.TABLE);
    }

    public void addColumn(@NonNls String objectName, @NonNls String dataType, boolean notNull, boolean primaryKey) {
        DBColumnFactoryInput column = new DBColumnFactoryInput(this, columns.size(), objectName, dataType, notNull, primaryKey);
        columns.add(column);
    }

    @Override
    public void validate(List<String> errors) {
        String objectName = getObjectName();
        DBObjectType objectType = getObjectType();

        if (objectName.isEmpty()) {
            String hint = getParent() == null ? "" : " at index " + getIndex();
            errors.add(objectType.getName() + " name is not specified" + hint);
            
        } else if (!Strings.isWord(objectName)) {
            errors.add("invalid " + objectType.getName() + " name specified" + ": \"" + objectName + "\"");
        }


        Set<String> columnNames = new HashSet<>();
        for (DBColumnFactoryInput column : columns) {
            column.validate(errors);
            String columnName = column.getObjectName();
            if (Strings.isEmptyOrSpaces(columnName)) continue; // already covered by field validator

            if (columnNames.contains(columnName)) {
                String hint = getParent() == null ? "" : " for " + objectType.getName() + " \"" + objectName + "\"";
                errors.add("duplicate column name \"" + columnName + "\"" + hint);
            }
            columnNames.add(columnName);
        }
    }
}
