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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.TableMetadataTool;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

import static com.dbn.common.util.Lists.convert;

public class TableMetadataToolImpl extends AssistantToolBase implements TableMetadataTool {

    @Override
    public List<String> listTableNames(String schemaName, boolean includeRegularTables, boolean includeTemporaryTables, String tableNameRegex) {
        DBSchema schema = getSchema(schemaName);

        List<DBTable> tables = schema.getTables();
        Predicate<DBTable> nameFilter = nameFilter(tableNameRegex);
        Predicate<DBTable> filter = t -> {
            if (!includeRegularTables && !t.isTemporary()) return false;
            if (!includeTemporaryTables && t.isTemporary()) return false;
            return nameFilter.test(t);
        };
        return getObjectNames(tables, false, filter);
    }

    @Override
    public TableDefinition loadTableDefinition(String schemaName, String tableName, boolean detailed) {
        DBSchema schema = getSchema(schemaName);
        return loadTableDefinition(schema, tableName, detailed);
    }

    @Override
    public List<TableDefinition> loadTableDefinitions(String schemaName, List<String> tableNames, boolean detailed) {
        DBSchema schema = getSchema(schemaName);
        return convert(tableNames, n -> loadTableDefinition(schema, n, detailed));
    }

    private TableDefinition loadTableDefinition(DBSchema schema, String tableName, boolean detailed) {
        DBTable table = schema.getTable(tableName);
        verify(table, DBObjectType.TABLE, tableName);

        TableDefinition tableDef = createDefinition(table);
        tableDef.setColumns(convert(undisposed(table).getColumns(), c -> createDefinition(c)));
        tableDef.setConstraints(convert(undisposed(table).getConstraints(), c -> createDefinition(c, detailed)));

        return tableDef;
    }

    private static @NotNull TableDefinition createDefinition(DBTable table) {
        TableDefinition tableDef = new TableDefinition();
        tableDef.setName(table.getQualifiedName());
        tableDef.setDescription(table.getComments());
        return tableDef;
    }

    private static ColumnDefinition createDefinition(DBColumn column) {
        ColumnDefinition columnDef = new ColumnDefinition();
        columnDef.setName(column.getName());
        columnDef.setType(column.getDataType().getName());
        columnDef.setDescription(column.getComments());
        return columnDef;
    }

    private static ConstraintDefinition createDefinition(DBConstraint constraint, boolean detailed) {
        if (constraint == null) return null;

        ConstraintDefinition constraintDef = new ConstraintDefinition();
        constraintDef.setName(constraint.getQualifiedName());
        constraintDef.setType(constraint.getConstraintType().getName());
        constraintDef.setCheckCondition(constraint.getCheckCondition());
        constraintDef.setColumns(getObjectNames(constraint.getColumns(), true));

        if (detailed) {
            DBConstraint fkConstraint = constraint.getForeignKeyConstraint();
            constraintDef.setForeignKeyConstraint(createDefinition(fkConstraint, false));
        }
        return constraintDef;
    }
}
