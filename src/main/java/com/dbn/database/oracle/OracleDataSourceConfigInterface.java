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

package com.dbn.database.oracle;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.interfaces.DatabaseDataSourceConfigInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OracleDataSourceConfigInterface extends DatabaseInterfaceBase implements DatabaseDataSourceConfigInterface {

    public OracleDataSourceConfigInterface(DatabaseInterfaces provider) {
        super("oracle_data_source_config_interface.xml", provider);
    }

    @Override
    public ResultSet loadDataSourceConfigEntry(String key, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "data-source-config-entry", key);
    }

    @Override
    public void insertDataSourceConfigEntry(String key, String value, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "insert-data-source-config-entry", key, value);
    }

    @Override
    public void updateDataSourceConfigEntry(String key, String value, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "update-data-source-config-entry", key, value);
    }

    @Override
    public void deleteDataSourceConfigEntry(String key, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "delete-data-source-config-entry", key);
    }
}
