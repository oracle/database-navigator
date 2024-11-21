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

import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.sqlite.adapter.SqliteMetadataResultSetRow;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.cache.CacheKey.key;
import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.database.sqlite.adapter.SqliteRawMetaData.RawForeignKeyInfo;
import static com.dbn.database.sqlite.adapter.SqliteRawMetaData.RawIndexDetailInfo;
import static com.dbn.database.sqlite.adapter.SqliteRawMetaData.RawIndexInfo;
import static com.dbn.database.sqlite.adapter.SqliteRawMetaData.RawTableInfo;

public abstract class SqliteConstraintInfoResultSetStub<T extends SqliteMetadataResultSetRow<T>> extends SqliteDatasetInfoResultSetStub<T> {

    SqliteConstraintInfoResultSetStub(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
        super(ownerName, datasetNames, connection);
    }

    SqliteConstraintInfoResultSetStub(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        super(ownerName, datasetName, connection);
    }

    @Override
    protected abstract void init(String ownerName, String datasetName) throws SQLException;

    protected abstract ResultSet loadTableInfo(String ownerName, String datasetName) throws SQLException;
    protected abstract ResultSet loadForeignKeyInfo(String ownerName, String datasetName) throws SQLException;
    protected abstract ResultSet loadIndexInfo(String ownerName, String tableName) throws SQLException;
    protected abstract ResultSet loadIndexDetailInfo(String ownerName, String indexName) throws SQLException;

    Map<String, List<SqliteConstraintsLoader.ConstraintColumnInfo>> loadConstraintInfo(final String ownerName, String datasetName) throws SQLException {
        SqliteConstraintsLoader loader = new SqliteConstraintsLoader(ownerName) {
            @Override
            public ResultSet loadTableInfo(String datasetName) throws SQLException {
                return SqliteConstraintInfoResultSetStub.this.loadTableInfo(ownerName, datasetName);
            }

            @Override
            public ResultSet loadForeignKeyInfo(String datasetName) throws SQLException {
                return SqliteConstraintInfoResultSetStub.this.loadForeignKeyInfo(ownerName, datasetName);
            }

            @Override
            public ResultSet loadIndexInfo(String tableName) throws SQLException {
                return SqliteConstraintInfoResultSetStub.this.loadIndexInfo(ownerName, tableName);
            }

            @Override
            public ResultSet loadIndexDetailInfo(String indexName) throws SQLException {
                return SqliteConstraintInfoResultSetStub.this.loadIndexDetailInfo(ownerName, indexName);
            }
        };
        return loader.loadConstraints(datasetName);
    }



    public abstract static class SqliteConstraintsLoader {
        private String ownerName;

        public enum ConstraintType {
            PK,
            FK,
            UQ
        }

        SqliteConstraintsLoader(String ownerName) {
            this.ownerName = ownerName;
        }

        @NotNull
        Map<String, List<ConstraintColumnInfo>> loadConstraints(final String dataset) throws SQLException {
            RawTableInfo tableInfo = getTableInfo(dataset);
            RawForeignKeyInfo foreignKeyInfo = getForeignKeyInfo(dataset);
            RawIndexInfo indexInfo = getIndexInfo(dataset);

            Map<String, List<ConstraintColumnInfo>> constraints = new HashMap<>();
            AtomicInteger pkPosition = new AtomicInteger();

            for (RawTableInfo.Row row : tableInfo.getRows()) {
                String column = row.getName();
                if (row.getPk() > 0) {
                    List<ConstraintColumnInfo> primaryKeyColumns = getConstraintColumns(constraints, "PK");
                    primaryKeyColumns.add(new ConstraintColumnInfo(dataset, column, null, null, pkPosition.get()));
                    pkPosition.incrementAndGet();
                }

            }

            for (RawForeignKeyInfo.Row row : foreignKeyInfo.getRows()) {
                String column = row.getFrom();
                String fkColumn = row.getTo();
                String fkDataset = row.getTable();
                int position = row.getSeq();
                String indexId = "FK" + row.getId();
                List<ConstraintColumnInfo> foreignKeyColumns = getConstraintColumns(constraints, indexId);
                foreignKeyColumns.add(new ConstraintColumnInfo(dataset, column, fkDataset, fkColumn, position));
            }

            for (RawIndexInfo.Row row : indexInfo.getRows()) {
                if (row.getUnique() == 1 && !Objects.equals(row.getOrigin(), "pk")) {
                    String indexId = "UQ" + row.getSeq();
                    String indexName = row.getName();
                    RawIndexDetailInfo detailInfo = getIndexDetailInfo(indexName);
                    for (RawIndexDetailInfo.Row detailRow : detailInfo.getRows()) {
                        String column = detailRow.getName();
                        if (Strings.isNotEmpty(column)) {
                            int position = detailRow.getSeqno();
                            List<ConstraintColumnInfo> uniqueKeyColumns = getConstraintColumns(constraints, indexId);
                            uniqueKeyColumns.add(new ConstraintColumnInfo(dataset, column, null, null, position));
                        }
                    }
                }
            }

            return constraints;
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

        private RawIndexInfo getIndexInfo(final String tableName) throws SQLException {
            return cache().get(
                    key(ownerName, tableName, "INDEX_INFO"),
                    () -> new RawIndexInfo(loadIndexInfo(tableName)));
        }

        private RawIndexDetailInfo getIndexDetailInfo(final String indexName) throws SQLException {
            return cache().get(
                    key(ownerName, indexName, "INDEX_DETAIL_INFO"),
                    () -> new RawIndexDetailInfo(loadIndexDetailInfo(indexName)));
        }

        @NotNull
        List<ConstraintColumnInfo> getConstraintColumns(Map<String, List<ConstraintColumnInfo>> constraints, String indexId) {
            return constraints.computeIfAbsent(indexId, id -> new ArrayList<>());
        }

        public abstract ResultSet loadTableInfo(String datasetName) throws SQLException;
        public abstract ResultSet loadForeignKeyInfo(String datasetName) throws SQLException;
        public abstract ResultSet loadIndexInfo(String tableName) throws SQLException;
        public abstract ResultSet loadIndexDetailInfo(String indexName) throws SQLException;

        public static String getConstraintName(ConstraintType constraintType, List<ConstraintColumnInfo> constraintColumnInfos) {
            StringBuilder builder = new StringBuilder();
            for (ConstraintColumnInfo constraintColumnInfo : constraintColumnInfos) {
                if (builder.length() == 0) {
                    builder.append(cachedLowerCase(constraintType.name()));
                    builder.append("_");
                    builder.append(constraintType == ConstraintType.FK ?
                            constraintColumnInfo.getFkDataset() :
                            constraintColumnInfo.getDataset());
                }
                builder.append("_");
                builder.append(constraintType == ConstraintType.FK ?
                        constraintColumnInfo.getFkColumn() :
                        constraintColumnInfo.getColumn());
            }
            return toLowerCase(builder);
        }

        public static class ConstraintColumnInfo {
            String dataset;
            String column;
            String fkDataset;
            String fkColumn;
            int position;

            ConstraintColumnInfo(String dataset, String column, String fkDataset, String fkColumn, int position) {
                this.dataset = dataset;
                this.column = column;
                this.fkDataset = fkDataset;
                this.fkColumn = fkColumn;
                this.position = position;
            }

            public String getDataset() {
                return dataset;
            }

            public String getColumn() {
                return column;
            }

            public String getFkDataset() {
                return fkDataset;
            }

            public String getFkColumn() {
                return fkColumn;
            }

            public int getPosition() {
                return position;
            }
        }
    }
}
