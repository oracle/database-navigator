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
import lombok.val;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.database.sqlite.adapter.rs.SqliteConstraintInfoResultSetStub.SqliteConstraintsLoader.ConstraintColumnInfo;
import static com.dbn.database.sqlite.adapter.rs.SqliteConstraintInfoResultSetStub.SqliteConstraintsLoader.ConstraintType;
import static com.dbn.database.sqlite.adapter.rs.SqliteConstraintInfoResultSetStub.SqliteConstraintsLoader.getConstraintName;

/**
 * DATASET_NAME,
 * CONSTRAINT_NAME,
 * CONSTRAINT_TYPE,
 * FK_CONSTRAINT_OWNER
 * FK_CONSTRAINT_NAME
 * IS_ENABLED
 * CHECK_CONDITION
 */

public abstract class SqliteConstraintsResultSet extends SqliteConstraintInfoResultSetStub<SqliteConstraintsResultSet.Constraint> {


    public SqliteConstraintsResultSet(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
        super(ownerName, datasetNames, connection);
    }

    public SqliteConstraintsResultSet(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        super(ownerName, datasetName, connection);
    }

    @Override
    protected void init(String ownerName, String datasetName) throws SQLException {
        Map<String, List<ConstraintColumnInfo>> constraints = loadConstraintInfo(ownerName, datasetName);
        for (val entry : constraints.entrySet()) {
            String indexKey = entry.getKey();
            List<ConstraintColumnInfo> columnInfos = entry.getValue();

            if (indexKey.startsWith("FK")) {
                String constraintName = getConstraintName(ConstraintType.FK, columnInfos);
                Constraint constraint = new Constraint();
                constraint.constraintName = constraintName;
                constraint.datasetName = datasetName;
                constraint.constraintType = "FOREIGN KEY";
                constraint.fkConstraintOwner = ownerName;
                constraint.fkConstraintName = constraintName.replace("fk_", "pk_");
                add(constraint);
            } else if (indexKey.startsWith("PK")) {
                String constraintName = getConstraintName(ConstraintType.PK, columnInfos);
                Constraint constraint = new Constraint();
                constraint.constraintName = constraintName;
                constraint.datasetName = datasetName;
                constraint.constraintType = "PRIMARY KEY";
                add(constraint);
            } else if (indexKey.startsWith("UQ")) {
                String constraintName = getConstraintName(ConstraintType.UQ, columnInfos);
                Constraint constraint = new Constraint();
                constraint.constraintName = constraintName;
                constraint.datasetName = datasetName;
                constraint.constraintType = "UNIQUE";
                add(constraint);
            }

        }
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Constraint element = current();
        return
            Objects.equals(columnLabel, "DATASET_NAME") ? element.datasetName :
            Objects.equals(columnLabel, "CONSTRAINT_NAME") ? element.constraintName :
            Objects.equals(columnLabel, "CONSTRAINT_TYPE") ? element.constraintType :
            Objects.equals(columnLabel, "CHECK_CONDITION") ? element.checkCondition :
            Objects.equals(columnLabel, "FK_CONSTRAINT_OWNER") ? element.fkConstraintOwner :
            Objects.equals(columnLabel, "FK_CONSTRAINT_NAME") ? element.fkConstraintName :
            Objects.equals(columnLabel, "IS_ENABLED") ? "Y" : null;
    }

    static class Constraint implements SqliteMetadataResultSetRow<Constraint> {
        private String datasetName;
        private String constraintName;
        private String constraintType;
        private String checkCondition;
        private String fkConstraintOwner;
        private String fkConstraintName;

        @Override
        public String identifier() {
            return datasetName + "." + constraintName;

        }

        @Override
        public String toString() {
            return "[CONSTRAINT] \"" + datasetName + "\".\"" + constraintName + "\"";
        }
    }
}
