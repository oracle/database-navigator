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

package com.dbn.data.model.resultSet;

import com.dbn.connection.ConnectionHandler;
import com.dbn.data.model.basic.BasicDataModelHeader;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

public class ResultSetDataModelHeader<T extends ResultSetColumnInfo> extends BasicDataModelHeader<T> {

    public ResultSetDataModelHeader() {
    }

    public ResultSetDataModelHeader(ConnectionHandler connection, ResultSet resultSet) throws SQLException {
        super();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            T columnInfo = createColumnInfo(connection, resultSet, i);
            addColumnInfo(columnInfo);
        }
    }

    public T getResultSetColumnInfo(int resultSetColumnIndex) {
        List<T> columnInfos = getColumnInfos();
        for (T columnInfo : columnInfos) {
            if (columnInfo.getResultSetIndex() == resultSetColumnIndex) {
                return columnInfo;
            }
        }
        throw new IllegalArgumentException("Invalid result set column index " + resultSetColumnIndex + ". Columns count " + columnInfos.size());
    }

    @NotNull
    public T createColumnInfo(ConnectionHandler connection, ResultSet resultSet, int columnIndex) throws SQLException {
        return (T) new ResultSetColumnInfo(connection, resultSet, columnIndex);
    }
}
