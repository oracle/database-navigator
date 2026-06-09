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
import com.dbn.connection.config.datasource.model.DataSourceConfigRecord;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataSourceConfigInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.common.Priority.HIGH;

public class DataSourceConfigStoreService {
    public @Nullable DataSourceConfigRecord loadRecord(@NotNull ConnectionHandler connection, @NotNull String key) throws SQLException {
        return DatabaseInterfaceInvoker.load(
                HIGH,
                "Loading configuration entry",
                "Loading configuration entry",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> doLoadRecord(connection.getDataSourceConfigInterface(), conn, key));
    }

    public void insertRecord(@NotNull ConnectionHandler connection, @NotNull String key, @NotNull String jsonValue) throws SQLException {
        DatabaseInterfaceInvoker.execute(
                HIGH,
                "Creating configuration entry",
                "Creating configuration entry",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> connection.getDataSourceConfigInterface().insertDataSourceConfigEntry(key, jsonValue, conn));
    }

    public void updateRecord(@NotNull ConnectionHandler connection, @NotNull String key, @NotNull String jsonValue) throws SQLException {
        DatabaseInterfaceInvoker.execute(
                HIGH,
                "Saving configuration entry",
                "Saving configuration entry",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> connection.getDataSourceConfigInterface().updateDataSourceConfigEntry(key, jsonValue, conn));
    }

    public void deleteRecord(@NotNull ConnectionHandler connection, @NotNull String key) throws SQLException {
        DatabaseInterfaceInvoker.execute(
                HIGH,
                "Deleting configuration entry",
                "Deleting configuration entry",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> connection.getDataSourceConfigInterface().deleteDataSourceConfigEntry(key, conn));
    }

    private DataSourceConfigRecord doLoadRecord(DatabaseDataSourceConfigInterface dataSourceConfig, DBNConnection conn, String key) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = dataSourceConfig.loadDataSourceConfigEntry(key, conn);
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
