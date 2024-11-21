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
import com.dbn.database.common.util.ResultSetReader;
import com.dbn.database.sqlite.adapter.SqliteMetadataResultSet;
import com.dbn.database.sqlite.adapter.SqliteMetadataResultSetRow;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
public abstract class SqliteDatasetInfoResultSetStub<T extends SqliteMetadataResultSetRow> extends SqliteMetadataResultSet<T> {
    private final DBNConnection connection;
    protected String ownerName;

    SqliteDatasetInfoResultSetStub(final String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
        this.connection = connection;
        this.ownerName = ownerName;
        new ResultSetReader(datasetNames) {
            @Override
            protected void processRow(ResultSet resultSet) throws SQLException {
                String datasetName = resultSet.getString("DATASET_NAME");
                init(ownerName, datasetName);
            }
        };
    }

    SqliteDatasetInfoResultSetStub(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        this.connection = connection;
        this.ownerName = ownerName;
        init(ownerName, datasetName);
    }

    protected abstract void init(String ownerName, String datasetName) throws SQLException;
}
