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

package com.dbn.editor.data.filter.global;

import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.DBTable;

public class GlobalDataFilter implements SelectStatementFilter{
    
    private DBTable filterTable;
    private DBColumn filterColumn;
    private Object filterValue;

    public GlobalDataFilter(DBColumn filterColumn, Object filterValue) {
        this.filterColumn = filterColumn;
        this.filterValue = filterValue;
        this.filterTable = (DBTable) filterColumn.getDataset();
    }

    public ConnectionHandler getConnection() {
        return filterColumn.getConnection();
    }

    public DBDataset getFilterTable() {
        return filterTable;
    }

    public DBColumn getFilterColumn() {
        return filterColumn;
    }

    public Object getFilterValue() {
        return filterValue;
    }

    @Override
    public String createSelectStatement(DBDataset dataset) {
        return null;
    }

    private void buildDependencyLink(DBTable fromTable, DBTable toTable) {
        for (DBColumn column : fromTable.getForeignKeyColumns()) {
            
        }
    }
}
