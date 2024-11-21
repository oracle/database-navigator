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
import com.dbn.data.model.basic.BasicColumnInfo;
import com.dbn.data.type.DBDataType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ResultSetColumnInfo extends BasicColumnInfo {
    private final int resultSetIndex;

    public ResultSetColumnInfo(ConnectionHandler connection, ResultSet resultSet, int index) throws SQLException {
        super(null, null, index);
        resultSetIndex = index + 1;
        ResultSetMetaData metaData = resultSet.getMetaData();
        name = translateName(metaData.getColumnName(resultSetIndex).intern(), connection);

        String dataTypeName = metaData.getColumnTypeName(resultSetIndex);
        int precision = getPrecision(metaData);
        int scale = metaData.getScale(resultSetIndex);

        dataType = DBDataType.get(connection, dataTypeName, precision, precision, scale, false);
    }

    public ResultSetColumnInfo(String name, DBDataType dataType, int columnIndex, int resultSetIndex) {
        super(name, dataType, columnIndex);
        this.resultSetIndex = resultSetIndex;
    }


    // lenient approach for oracle bug returning the size of LOBs instead of the precision.
    private int getPrecision(ResultSetMetaData metaData) throws SQLException {
        try {
            return metaData.getPrecision(resultSetIndex);
        } catch (NumberFormatException e) {
            conditionallyLog(e);
            return 4000;
        }
    }

    public String translateName(String name, ConnectionHandler connection) {
        return name;
    }
}
