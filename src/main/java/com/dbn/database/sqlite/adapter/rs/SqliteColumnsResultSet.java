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

package com.dbn.database.sqlite.adapter.rs;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.sqlite.adapter.SqliteMetadataResultSetRow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.common.cache.CacheKey.key;
import static com.dbn.database.sqlite.adapter.SqliteRawMetaData.RawForeignKeyInfo;
import static com.dbn.database.sqlite.adapter.SqliteRawMetaData.RawTableInfo;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * COLUMN_NAME
 * POSITION
 * DATA_TYPE_NAME
 * DECL_TYPE_NAME
 * DECL_TYPE_OWNER
 * DECL_TYPE_PROGRAM
 * DATA_LENGTH
 * DATA_PRECISION
 * DATA_SCALE
 * IS_SET
 * IS_NULLABLE
 * IS_HIDDEN
 * IS_PRIMARY_KEY
 * IS_FOREIGN_KEY
 * IS_UNIQUE_KEY
 * IS_IDENTITY
 */

public abstract class SqliteColumnsResultSet extends SqliteDatasetInfoResultSetStub<SqliteColumnsResultSet.Column> {

    protected SqliteColumnsResultSet(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
        super(ownerName, datasetNames, connection);
    }

    protected SqliteColumnsResultSet(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        super(ownerName, datasetName, connection);
    }

    @Override
    protected void init(String ownerName, String datasetName) throws SQLException {
        RawTableInfo tableInfo = getTableInfo(datasetName);

        for (RawTableInfo.Row row : tableInfo.getRows()) {
            Column element = new Column();
            element.datasetName =  datasetName;
            element.columnName = row.getName();
            element.dataTypeName = row.getType();
            element.dataLength = row.getLength();
            element.dataPrecision = row.getPrecision();
            element.dataScale = row.getScale();
            element.nullable = row.getNotnull() == 0;
            element.primaryKey = row.getPk() > 0;
            element.position = row.getCid() + 1;
            add(element);
        }

        try {
            RawForeignKeyInfo foreignKeyInfo = getForeignKeyInfo(datasetName);
            for (RawForeignKeyInfo.Row row : foreignKeyInfo.getRows()) {
                String columnName = row.getFrom();
                Column column = row(datasetName + "." + columnName);
                column.foreignKey = true;
            }
        } catch (SQLException e) {
            conditionallyLog(e);
        }
    }

    private RawForeignKeyInfo getForeignKeyInfo(final String datasetName) throws SQLException {
        return cache().get(
                key(ownerName, datasetName, "FOREIGN_KEY_INFO"),
                () -> new RawForeignKeyInfo(loadForeignKeyInfo(datasetName)));
    }

    private RawTableInfo getTableInfo(final String datasetName) throws SQLException {
        return cache().get(
                key(ownerName, datasetName, "TABLE_INFO"),
                () -> new RawTableInfo(loadTableInfo(datasetName)));
    }

    protected abstract ResultSet loadTableInfo(String datasetName) throws SQLException;
    protected abstract ResultSet loadForeignKeyInfo(String datasetName) throws SQLException;

    @Override
    public String getString(String columnLabel) throws SQLException {
        Column element = current();
            return
                Objects.equals(columnLabel, "DATASET_NAME") ? element.datasetName :
                Objects.equals(columnLabel, "COLUMN_NAME") ? element.columnName :
                Objects.equals(columnLabel, "DATA_TYPE_NAME") ? element.dataTypeName :
                Objects.equals(columnLabel, "IS_FOREIGN_KEY") ? toFlag(element.foreignKey) :
                Objects.equals(columnLabel, "IS_UNIQUE_KEY") ? toFlag(element.uniqueKey) :
                Objects.equals(columnLabel, "IS_IDENTITY") ? "N" :
                Objects.equals(columnLabel, "IS_HIDDEN") ? "N" :
                Objects.equals(columnLabel, "IS_SET") ? "N" :
                Objects.equals(columnLabel, "IS_NULLABLE") ? toFlag(element.nullable) :
                Objects.equals(columnLabel, "IS_PRIMARY_KEY") ? toFlag(element.primaryKey) : null;
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        Column element = current();
        return(short) (
                Objects.equals(columnLabel, "POSITION") ? element.position :
                Objects.equals(columnLabel, "DATA_LENGTH") ? element.dataLength :
                Objects.equals(columnLabel, "DATA_PRECISION") ? element.dataPrecision :
                Objects.equals(columnLabel, "DATA_SCALE") ? element.dataScale : 0);
    }


    @Override
    public int getInt(String columnLabel) throws SQLException {
        Column element = current();
        return
            Objects.equals(columnLabel, "POSITION") ? element.position :
            Objects.equals(columnLabel, "DATA_LENGTH") ? element.dataLength :
            Objects.equals(columnLabel, "DATA_PRECISION") ? element.dataPrecision :
            Objects.equals(columnLabel, "DATA_SCALE") ? element.dataScale : 0;
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getInt(columnLabel);
    }

    static class Column implements SqliteMetadataResultSetRow<Column> {
        private String datasetName;
        private String columnName;
        private String dataTypeName;
        private int dataLength;
        private int dataPrecision;
        private int dataScale;
        private int position;

        private boolean nullable;
        private boolean primaryKey;
        private boolean foreignKey;
        private boolean uniqueKey;

        @Override
        public String identifier() {
            return datasetName + "." + columnName;
        }

        @Override
        public String toString() {
            return "[COLUMN] \"" + datasetName + "\".\"" + columnName + "\"";
        }
    }
}
