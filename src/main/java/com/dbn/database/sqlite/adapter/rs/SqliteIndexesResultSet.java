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

import com.dbn.common.cache.CacheKey;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.sqlite.adapter.SqliteMetadataResultSetRow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.database.sqlite.adapter.SqliteRawMetaData.RawIndexInfo;


/**
 * TABLE_NAME,
 * INDEX_NAME,
 * IS_UNIQUE,
 * IS_VALID
 */

public abstract class SqliteIndexesResultSet extends SqliteDatasetInfoResultSetStub<SqliteIndexesResultSet.Index> {

    protected SqliteIndexesResultSet(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
        super(ownerName, datasetNames, connection);
    }

    protected SqliteIndexesResultSet(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        super(ownerName, datasetName, connection);
    }

    @Override
    protected void init(String ownerName, String tableName) throws SQLException {
        RawIndexInfo indexInfo = getIndexInfo(tableName);

        for (RawIndexInfo.Row row : indexInfo.getRows()) {
            Index element = new Index();
            element.tableName = tableName;
            element.indexName = row.getName();
            element.unique = row.getUnique() == 1;
            element.valid = true;
            add(element);
        }
    }

    private RawIndexInfo getIndexInfo(final String tableName) throws SQLException {
        return cache().get(
                CacheKey.key(ownerName, tableName, "INDEX_INFO"),
                () -> new RawIndexInfo(loadIndexInfo(tableName)));
    }

    protected abstract ResultSet loadIndexInfo(String tableName) throws SQLException;

    @Override
    public String getString(String columnLabel) throws SQLException {
        Index index = current();
        return
            Objects.equals(columnLabel, "TABLE_NAME") ? index.tableName :
            Objects.equals(columnLabel, "INDEX_NAME") ? index.indexName :
            Objects.equals(columnLabel, "IS_UNIQUE") ? toFlag(index.unique) :
            Objects.equals(columnLabel, "IS_VALID") ? "Y" : null;
    }


    static class Index implements SqliteMetadataResultSetRow<Index> {
        private String tableName;
        private String indexName;
        private boolean unique;
        private boolean valid;

        @Override
        public String identifier() {
            return tableName + "." + indexName;
        }

        @Override
        public String toString() {
            return "[INDEX] \"" + tableName + "\".\"" + indexName + "\"";
        }
    }
}
