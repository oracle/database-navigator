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

package com.dbn.connection.config.datasource.service;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.config.datasource.model.DataSourceConfigEntry;
import com.dbn.connection.config.datasource.model.DataSourceConfigRecord;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.Priority.HIGH;

public class DataSourceConfigStoreService {
    public @NotNull List<DataSourceConfigEntry> loadEntries(@NotNull ConnectionHandler connection) throws SQLException {
        return DatabaseInterfaceInvoker.load(
                HIGH,
                "Loading data source config keys",
                "Loading data source config keys",
                connection.getProject(),
                connection.getConnectionId(),
                this::doLoadEntries);
    }

    public @Nullable DataSourceConfigRecord loadRecord(@NotNull ConnectionHandler connection, @NotNull String key) throws SQLException {
        return DatabaseInterfaceInvoker.load(
                HIGH,
                "Loading data source config",
                "Loading data source config",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> doLoadRecord(connection.getMetadataInterface(), conn, key));
    }

    public void insertRecord(@NotNull ConnectionHandler connection, @NotNull String key, @NotNull String jsonValue) throws SQLException {
        DatabaseInterfaceInvoker.execute(
                HIGH,
                "Saving data source config",
                "Saving data source config",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> connection.getMetadataInterface().insertDataSourceConfigEntry(key, jsonValue, conn));
    }

    public void updateRecord(@NotNull ConnectionHandler connection, @NotNull String key, @NotNull String jsonValue) throws SQLException {
        DatabaseInterfaceInvoker.execute(
                HIGH,
                "Updating data source config",
                "Updating data source config",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> connection.getMetadataInterface().updateDataSourceConfigEntry(key, jsonValue, conn));
    }

    public void deleteRecord(@NotNull ConnectionHandler connection, @NotNull String key) throws SQLException {
        DatabaseInterfaceInvoker.execute(
                HIGH,
                "Deleting data source config",
                "Deleting data source config",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> connection.getMetadataInterface().deleteDataSourceConfigEntry(key, conn));
    }

    private List<DataSourceConfigEntry> doLoadEntries(DBNConnection conn) throws SQLException {
        List<DataSourceConfigEntry> entries = new ArrayList<>();
        ResultSet resultSet = null;
        DatabaseMetadataInterface metadata = conn.getConnectionHandler().getMetadataInterface();
        try {
            resultSet = metadata.loadDataSourceConfigEntries(conn);
            while (resultSet.next()) {
                entries.add(new DataSourceConfigEntry(
                        resultSet.getString("key"),
                        resultSet.getString("last_updated")));
            }
            return entries;
        } finally {
            Resources.close(resultSet);
        }
    }

    private DataSourceConfigRecord doLoadRecord(DatabaseMetadataInterface metadata, DBNConnection conn, String key) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = metadata.loadDataSourceConfigEntry(key, conn);
            if (resultSet.next()) {
                return new DataSourceConfigRecord(
                        resultSet.getString("key"),
                        resultSet.getString("value"),
                        resultSet.getString("last_updated"));
            }
            return null;
        } finally {
            Resources.close(resultSet);
        }
    }
}
