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

package com.dbn.object.management.adapter.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.object.DBDataSourceConfigEntry;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.common.constant.Constant.array;
import static com.dbn.object.type.DBObjectType.DATA_SOURCE_CONFIG_ENTRY;

public class DBDataSourceConfigEntryManagementAdapter extends ObjectManagementAdapterBase<DBDataSourceConfigEntry> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return array(DATA_SOURCE_CONFIG_ENTRY);
    }

    @Override
    protected void createObject(ConnectionHandler connection, DBNConnection conn, DBDataSourceConfigEntry object) throws SQLException {
        connection.getDataSourceConfigInterface().insertDataSourceConfigEntry(
                object.getName(),
                object.getValue(),
                conn);
    }

    @Override
    protected void deleteObject(ConnectionHandler connection, DBNConnection conn, DBDataSourceConfigEntry object) throws SQLException {
        connection.getDataSourceConfigInterface().deleteDataSourceConfigEntry(object.getName(), conn);
    }
}
