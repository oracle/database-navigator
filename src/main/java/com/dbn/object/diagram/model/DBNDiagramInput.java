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

package com.dbn.object.diagram.model;

import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.load.ProgressMonitor.checkCancelled;
import static com.dbn.common.load.ProgressMonitor.setProgressDetail;

public final class DBNDiagramInput {
    private final List<DBObjectRef<DBTable>> tables;
    private final Map<DBObjectRef<DBTable>, List<DBObjectRef<DBColumn>>> columns;

    public DBNDiagramInput(@NotNull Collection<? extends DBTable> tables) {
        List<DBObjectRef<DBTable>> tableRefs = new ArrayList<>(tables.size());
        Map<DBObjectRef<DBTable>, List<DBObjectRef<DBColumn>>> columnIndex = new LinkedHashMap<>();
        for (DBTable table : tables) {
            DBObjectRef<DBTable> tableRef = DBObjectRef.of(table);
            tableRefs.add(tableRef);
            List<DBObjectRef<DBColumn>> columnRefs = new ArrayList<>();
            for (DBColumn column : table.getColumns()) {
                columnRefs.add(DBObjectRef.of(column));
            }
            columnIndex.put(tableRef, Collections.unmodifiableList(columnRefs));
        }
        this.tables = Collections.unmodifiableList(tableRefs);
        this.columns = Collections.unmodifiableMap(columnIndex);
    }

    public DBNDiagramInput(@NotNull DBTable rootTable) {
        this(findRelatedTables(rootTable));
    }

    private static Collection<DBTable> findRelatedTables(DBTable rootTable) {
        List<DBTable> tables = new ArrayList<>();
        Set<DBObjectRef<DBTable>> visited = new LinkedHashSet<>();
        Deque<DBTable> pending = new ArrayDeque<>();
        pending.add(rootTable);
        while (!pending.isEmpty()) {
            DBTable table = pending.removeFirst();
            DBObjectRef<DBTable> tableRef = DBObjectRef.of(table);
            if (!visited.add(tableRef)) continue;

            checkCancelled();
            setProgressDetail("Exploring table " + table.getQualifiedName());
            tables.add(table);
            for (DBColumn column : table.getColumns()) {
                addRelatedTable(pending, column.getForeignKeyColumn());
                if (!column.isPrimaryKey()) continue;

                for (DBColumn referencingColumn : column.getReferencingColumns()) {
                    addRelatedTable(pending, referencingColumn);
                }
            }
        }
        return tables;
    }

    private static void addRelatedTable(Deque<DBTable> pending, DBColumn column) {
        if (column == null) return;

        DBDataset dataset = column.getDataset();
        if (dataset instanceof DBTable table) pending.addLast(table);
    }

    @NotNull
    public List<DBTable> getTables() {
        List<DBTable> result = new ArrayList<>(tables.size());
        for (DBObjectRef<DBTable> tableRef : tables) {
            DBTable table = tableRef.get();
            if (table != null) result.add(table);
        }
        return Collections.unmodifiableList(result);
    }

    public DBTable getTable(@NotNull DBTable table) {
        DBObjectRef<DBTable> tableRef = findTableRef(table);
        return tableRef == null ? null : tableRef.get();
    }

    @NotNull
    public List<DBColumn> getColumns(@NotNull DBTable table) {
        List<DBObjectRef<DBColumn>> columnRefs = columns.get(table.ref());
        if (columnRefs == null) return Collections.emptyList();
        List<DBColumn> result = new ArrayList<>(columnRefs.size());
        for (DBObjectRef<DBColumn> columnRef : columnRefs) {
            DBColumn column = columnRef.get();
            if (column != null) result.add(column);
        }
        return Collections.unmodifiableList(result);
    }

    private DBObjectRef<DBTable> findTableRef(DBTable table) {
        for (DBObjectRef<DBTable> tableRef : tables) {
            if (tableRef.equals(table.ref())) return tableRef;
        }
        return null;
    }

    @NotNull
    public List<DBTable> getDatabaseTables() {
        List<DBTable> result = new ArrayList<>(tables.size());
        for (DBObjectRef<DBTable> tableRef : tables) {
            DBTable databaseTable = tableRef.get();
            if (databaseTable != null) result.add(databaseTable);
        }
        return result;
    }
}
