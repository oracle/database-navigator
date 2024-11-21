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

package com.dbn.data.record;


import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.intellij.openapi.Disposable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Lists.isLast;

public class DatasetRecord implements Disposable {
    private DatasetFilterInput filterInput;
    private Map<String, Object> values = new HashMap<>();

    public DatasetRecord(DatasetFilterInput filterInput) throws SQLException {
        this.filterInput = filterInput;
        loadRecordValues(filterInput);
    }

    public DatasetFilterInput getFilterInput() {
        return filterInput;
    }

    private void loadRecordValues(DatasetFilterInput filterInput) throws SQLException {
        DBDataset dataset = getDataset();
        StringBuilder selectStatement = new StringBuilder();
        selectStatement.append("select ");

        List<DBColumn> columns = dataset.getColumns();
        for (DBColumn column : columns) {
            selectStatement.append(column.getName());
            if (!isLast(columns, column)) {
                selectStatement.append(", ");
            }
        }

        selectStatement.append(" from ");
        selectStatement.append(dataset.getQualifiedName());
        selectStatement.append(" where ");

        List<DBColumn> filterColumns = filterInput.getColumns();
        for (DBColumn column : filterColumns) {
            selectStatement.append(column.getName());
            selectStatement.append(" = ? ");
            if (!isLast(filterColumns, column)) {
                selectStatement.append(" and ");
            }
        }

        ConnectionHandler connection = dataset.getConnection();
        PooledConnection.run(connection.createConnectionContext(), conn -> {
            DBNPreparedStatement statement = null;
            DBNResultSet resultSet = null;
            try {
                statement = conn.prepareStatement(selectStatement.toString());

                int index = 1;

                for (DBColumn column : filterColumns) {
                    Object value = filterInput.getColumnValue(column);
                    DBDataType dataType = column.getDataType();
                    dataType.setValueToPreparedStatement(statement, index, value);
                    index++;
                }

                resultSet = statement.executeQuery();
                try {
                    if (resultSet.next()) {
                        index = 1;

                        for (DBColumn column : dataset.getColumns()) {
                            DBDataType dataType = column.getDataType();
                            Object value = dataType.getValueFromResultSet(resultSet, index);
                            values.put(column.getName(), value);
                            index++;
                        }
                    }
                } finally {
                    conn.updateLastAccess();
                }
            } finally {
                Resources.close(resultSet);
                Resources.close(statement);
            }
        });
    }

    public DBDataset getDataset() {
        return filterInput.getDataset();
    }

    public Object getColumnValue(DBColumn column) {
        return values.get(column.getName());
    }

    @Override
    public void dispose() {
        filterInput = null;
        values.clear();
        values = null;
    }
}
