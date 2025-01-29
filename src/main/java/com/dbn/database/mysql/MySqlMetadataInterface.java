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

package com.dbn.database.mysql;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseMetadataInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import org.jetbrains.annotations.NonNls;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;


public class MySqlMetadataInterface extends DatabaseMetadataInterfaceImpl {

    MySqlMetadataInterface(DatabaseInterfaces provider) {
        super("mysql_metadata_interface.xml", provider);
    }

    @Override
    public ResultSet loadCompileObjectErrors(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        return null;
    }

    @Override
    public ResultSet loadMethodArguments(String ownerName, String methodName, String methodType, short overload, DBNConnection connection) throws SQLException {
        try {
            return super.loadMethodArguments(ownerName, methodName, methodType, overload, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            ResultSet resultSet = executeQuery(connection, "alternative-method-arguments", ownerName, methodName, methodType, overload);
            return new MySqlArgumentsResultSet(resultSet);
        }
    }

    @Override
    public ResultSet loadAllMethodArguments(String ownerName, DBNConnection connection) throws SQLException {
        try {
            return super.loadAllMethodArguments(ownerName, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            ResultSet resultSet = executeQuery(connection, "alternative-all-method-arguments", ownerName);
            return new MySqlArgumentsResultSet(resultSet);
        }
    }

    @Override
    @NonNls
    public String createDateString(Date date) {
        String dateString = META_DATE_FORMAT.get().format(date);
        return "str_to_date('" + dateString + "', '%Y-%m-%d %T')";
    }

    @Override
    public void terminateSession(Object sessionId, Object serialNumber, boolean immediate, DBNConnection connection) throws SQLException {
        executeStatement(connection, "kill-session", sessionId);
    }
}