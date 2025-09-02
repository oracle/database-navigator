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
import com.dbn.common.util.Lists;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.type.DBObjectType;

import java.util.List;

public class TableMetadataToolImpl extends AssistantToolBase implements com.dbn.assistant.tool.spec.TableMetadataTool {

    @Override
    public List<String> listTableNames(String schemaName, boolean includeTemporaryTables) {
        DBSchema schema = getSchema(schemaName);

        List<DBTable> tables = schema.getTables();
        return getObjectNames(tables, t -> includeTemporaryTables || !t.isTemporary());
    }

    @Override
    public TableDefinition loadTableDefinition(String schemaName, String tableName) {
        DBSchema schema = getSchema(schemaName);
        return loadTableDefinition(schema, tableName);
    }

    @Override
    public List<TableDefinition> loadTableDefinitions(String schemaName, List<String> tableNames) {
        DBSchema schema = getSchema(schemaName);
        return Lists.convert(tableNames, n -> loadTableDefinition(schema, n));
    }

    private TableDefinition loadTableDefinition(DBSchema schema, String tableName) {
        DBTable table = schema.getTable(tableName);
        table = assertNotNull(table, DBObjectType.TABLE, tableName);

        TableDefinition tableDef = new TableDefinition();
        List<ColumnDefinition> columns = tableDef.getColumns();
        for (DBColumn column : table.getColumns()) {
            ColumnDefinition columnDef = new ColumnDefinition();
            columnDef.setName(column.getName());
            columnDef.setName(column.getTypeName());
            columnDef.setDescription(column.getDescription());

            columns.add(columnDef);
        }

        return tableDef;
    }
}
