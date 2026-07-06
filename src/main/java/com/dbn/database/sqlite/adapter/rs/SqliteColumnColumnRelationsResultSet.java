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

package com.dbn.database.sqlite.adapter.rs;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.sqlite.adapter.SqliteMetadataResultSetRow;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.database.sqlite.adapter.rs.SqliteConstraintInfoResultSetStub.SqliteConstraintsLoader.ConstraintColumnInfo;

public abstract class SqliteColumnColumnRelationsResultSet extends SqliteConstraintInfoResultSetStub<SqliteColumnColumnRelationsResultSet.ColumnRelation> {
    public SqliteColumnColumnRelationsResultSet(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
        super(ownerName, datasetNames, connection);
    }

    public SqliteColumnColumnRelationsResultSet(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        super(ownerName, datasetName, connection);
    }

    @Override
    protected void init(String ownerName, String datasetName) throws SQLException {
        Map<String, List<ConstraintColumnInfo>> constraints = loadConstraintInfo(ownerName, datasetName);
        for (var entry : constraints.entrySet()) {
            if (!entry.getKey().startsWith("FK")) continue;

            for (ConstraintColumnInfo info : entry.getValue()) {
                ColumnRelation relation = new ColumnRelation();
                relation.sourceSchemaName = ownerName;
                relation.sourceDatasetName = info.getDataset();
                relation.sourceColumnName = info.getColumn();
                relation.targetSchemaName = ownerName;
                relation.targetDatasetName = info.getFkDataset();
                relation.targetColumnName = info.getFkColumn();
                add(relation);
            }
        }
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        ColumnRelation relation = current();
        return Objects.equals(columnLabel, "SOURCE_SCHEMA_NAME") ? relation.sourceSchemaName :
               Objects.equals(columnLabel, "SOURCE_DATASET_NAME") ? relation.sourceDatasetName :
               Objects.equals(columnLabel, "SOURCE_COLUMN_NAME") ? relation.sourceColumnName :
               Objects.equals(columnLabel, "TARGET_SCHEMA_NAME") ? relation.targetSchemaName :
               Objects.equals(columnLabel, "TARGET_DATASET_NAME") ? relation.targetDatasetName :
               Objects.equals(columnLabel, "TARGET_COLUMN_NAME") ? relation.targetColumnName : null;
    }

    static class ColumnRelation implements SqliteMetadataResultSetRow<ColumnRelation> {
        private String sourceSchemaName;
        private String sourceDatasetName;
        private String sourceColumnName;
        private String targetSchemaName;
        private String targetDatasetName;
        private String targetColumnName;

        @Override
        public String identifier() {
            return sourceDatasetName + '.' + sourceColumnName;
        }

        @Override
        public int compareTo(ColumnRelation other) {
            return identifier().compareTo(other.identifier());
        }
    }
}
