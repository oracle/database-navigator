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

package com.dbn.connection;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.ResourceStatus;
import com.dbn.database.interfaces.DatabaseInterface.ConnectionCallable;
import com.dbn.database.interfaces.DatabaseInterface.ConnectionRunnable;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class PooledConnection {
    private PooledConnection() {}

    public static void run(@NotNull ConnectionContext context, @NotNull ConnectionRunnable runnable) throws SQLException {
        ConnectionHandler connection = context.getConnection();
        SchemaId schemaId = context.getSchemaId();

        DBNConnection conn = null;
        try {
            conn = schemaId == null ?
                    connection.getPoolConnection(true) :
                    connection.getPoolConnection(schemaId, true);

            connection.checkDisposed();
            conn.set(ResourceStatus.ACTIVE, true);
            runnable.run(conn);
        } finally {
            if (conn != null) {
                connection.freePoolConnection(conn);
                conn.set(ResourceStatus.ACTIVE, false);
            }
        }
    }

    public static <T> T call(@NotNull ConnectionContext context, @NotNull ConnectionCallable<T> callable) throws SQLException {
        ConnectionHandler connection = context.getConnection();
        SchemaId schemaId = context.getSchemaId();

        DBNConnection c = null;
        try {
            c = schemaId == null ?
                    connection.getPoolConnection(true) :
                    connection.getPoolConnection(schemaId, true);

            connection.checkDisposed();
            c.set(ResourceStatus.ACTIVE, true);
            return callable.call(c);
        } finally {
            if (c != null) {
                connection.freePoolConnection(c);
                c.set(ResourceStatus.ACTIVE, false);
            }
        }
    }
}
