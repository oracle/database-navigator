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

import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

import java.util.List;

@Getter
@Setter
public class DBTableFactoryInput extends DBSchemaObjectFactoryInput {
    private DBObjectFactoryInputList<DBColumnFactoryInput> columns = new DBObjectFactoryInputList<>(this);
    private DBObjectFactoryInputList<DBConstraintFactoryInput> constraints = new DBObjectFactoryInputList<>(this);
    private String appendix;

    public DBTableFactoryInput(DBSchema schema) {
        super(schema, DBObjectType.TABLE);
    }

    public void addColumn(@NonNls String columnName, @NonNls String dataType, boolean notNull, boolean primaryKey) {
        DBColumnFactoryInput column = new DBColumnFactoryInput(this, columns.size(), columnName, dataType, notNull, primaryKey);
        columns.add(column);
    }

    public void addConstraint(@NonNls String constraintType, @NonNls String constraintName, List<String> columnNames) {
        DBConstraintFactoryInput constraint = new DBConstraintFactoryInput(this, constraints.size(), constraintType, constraintName, columnNames);
        constraints.add(constraint);
    }
}
