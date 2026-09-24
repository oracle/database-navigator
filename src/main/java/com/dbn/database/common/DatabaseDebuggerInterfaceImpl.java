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

package com.dbn.database.common;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.debug.DebuggerIdentifierInfo;
import com.dbn.editor.data.model.ResultSetSupport;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;

import java.sql.SQLException;
import java.util.List;

public abstract class DatabaseDebuggerInterfaceImpl extends DatabaseInterfaceBase implements DatabaseDebuggerInterface {
    public DatabaseDebuggerInterfaceImpl(String fileName, DatabaseInterfaces provider) {
        super(fileName, provider);
    }

    @Override
    public List<DebuggerIdentifierInfo> loadObjectIdentifiers(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException {
        return ResultSetSupport.consume(
                () -> executeQuery(connection, "object-identifiers", ownerName, objectName, objectType),
                rs -> DebuggerIdentifierInfo.read(rs));
    }

}
