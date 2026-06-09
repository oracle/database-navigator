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

package com.dbn.database.interfaces;

import com.dbn.connection.jdbc.DBNConnection;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.database.interfaces.DatabaseInterfaceType.DATA_SOURCE_CONFIG;

/**
 * Provides database-specific operations for DATA_SOURCE_CONFIG_STORE entries.
 */
public interface DatabaseDataSourceConfigInterface extends DatabaseInterface {
    @Override
    default DatabaseInterfaceType getInterfaceType() {
        return DATA_SOURCE_CONFIG;
    }

    ResultSet loadDataSourceConfigEntries(DBNConnection connection) throws SQLException;

    ResultSet loadDataSourceConfigEntry(String key, DBNConnection connection) throws SQLException;

    void insertDataSourceConfigEntry(String key, String value, DBNConnection connection) throws SQLException;

    void updateDataSourceConfigEntry(String key, String value, DBNConnection connection) throws SQLException;

    void deleteDataSourceConfigEntry(String key, DBNConnection connection) throws SQLException;
}
