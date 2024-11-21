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

package com.dbn.editor.session.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.editor.session.SessionBrowserFilterType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionBrowserColumnInfo extends ResultSetColumnInfo{
    public SessionBrowserColumnInfo(ConnectionHandler connection, ResultSet resultSet, int columnIndex) throws SQLException {
        super(connection, resultSet, columnIndex);
    }

    @Override
    public String translateName(String columnName, ConnectionHandler connection) {
        DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
        return compatibility.getSessionBrowserColumnName(columnName);
    }

    public SessionBrowserFilterType getFilterType() {
        String name = getName();
        if ("USER".equalsIgnoreCase(name)) return SessionBrowserFilterType.USER;
        if ("HOST".equalsIgnoreCase(name)) return SessionBrowserFilterType.HOST;
        if ("STATUS".equalsIgnoreCase(name)) return SessionBrowserFilterType.STATUS;
        return null;
    }
}
