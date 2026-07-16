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
import com.dbn.database.interfaces.DatabaseDatasourceConfigInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatasourceConfigCreationScope;

import java.sql.SQLException;

public class OracleDatasourceConfigInterface extends DatabaseInterfaceBase implements DatabaseDatasourceConfigInterface {

    public OracleDatasourceConfigInterface(DatabaseInterfaces provider) {
        super("oracle_datasource_config_interface.xml", provider);
    }

    @Override
    public String loadDatasourceConfigValue(String ownerName, String configName, DBNConnection connection) throws SQLException {
        return getSingleValue(connection, "datasource-config", ownerName, configName);
    }

    @Override
    public DatasourceConfigCreationScope loadDatasourceConfigCreationScope(DBNConnection connection) throws SQLException {
        return DatasourceConfigCreationScope.valueOf(getSingleValue(connection, "datasource-config-creation-scope"));
    }

    @Override
    public void createDatasourceConfig(String ownerName, String configName, String value, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "create-datasource-config", qualifiedName(ownerName, configName), value);
    }

    @Override
    public void updateDatasourceConfig(String ownerName, String configName, String value, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "update-datasource-config", qualifiedName(ownerName, configName), value);
    }

    @Override
    public void deleteDatasourceConfig(String ownerName, String configName, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "delete-datasource-config", qualifiedName(ownerName, configName));
    }

    private static String qualifiedName(String ownerName, String configName) {
        return ownerName + "." + configName;
    }
}
