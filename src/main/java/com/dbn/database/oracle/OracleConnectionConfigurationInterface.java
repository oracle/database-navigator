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
import com.dbn.database.interfaces.DatabaseConnectionConfigurationInterface;
import com.dbn.database.interfaces.ConnectionConfigurationCreationScope;
import com.dbn.database.interfaces.DatabaseInterfaces;

import java.sql.SQLException;

public class OracleConnectionConfigurationInterface extends DatabaseInterfaceBase implements DatabaseConnectionConfigurationInterface {

    public OracleConnectionConfigurationInterface(DatabaseInterfaces provider) {
        super("oracle_connection_configuration_interface.xml", provider);
    }

    @Override
    public String loadConnectionConfigurationValue(String ownerName, String configName, DBNConnection connection) throws SQLException {
        return getSingleValue(connection, "connection-configuration", ownerName, configName);
    }

    @Override
    public ConnectionConfigurationCreationScope loadConnectionConfigurationCreationScope(DBNConnection connection) throws SQLException {
        return ConnectionConfigurationCreationScope.valueOf(getSingleValue(connection, "connection-configuration-creation-scope"));
    }

    @Override
    public void createConnectionConfiguration(String qualifiedConfigName, String value, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "create-connection-configuration", qualifiedConfigName, value);
    }

    @Override
    public void updateConnectionConfiguration(String qualifiedConfigName, String value, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "update-connection-configuration", qualifiedConfigName, value);
    }

    @Override
    public void deleteConnectionConfiguration(String qualifiedConfigName, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "delete-connection-configuration", qualifiedConfigName);
    }
}
