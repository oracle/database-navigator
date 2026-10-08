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

package com.dbn.editor.data.nested;

import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModelHeader;
import com.dbn.object.DBColumn;
import com.dbn.object.DBNestedTable;

import java.util.List;

class DBNestedTableViewerModelHeader extends ResultSetDataModelHeader<ResultSetColumnInfo> {
    DBNestedTableViewerModelHeader(DBNestedTable nestedTable) {
        List<DBColumn> primaryKeyColumns = nestedTable.getParentTable().getPrimaryKeyColumns();
        int columnIndex = 0;

        if (!primaryKeyColumns.isEmpty()) {
            for (DBColumn column : primaryKeyColumns) {
                addColumn(column, columnIndex++);
            }
        }

        for (DBColumn column : nestedTable.getColumns()) {
            addColumn(column, columnIndex++);
        }
    }

    private void addColumn(DBColumn column, int columnIndex) {
        addColumnInfo(new ResultSetColumnInfo(
                column.getName(),
                column.getDataType(),
                columnIndex,
                columnIndex + 1));
    }
}
