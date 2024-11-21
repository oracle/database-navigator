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
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.interfaces.DatabaseInterface.Callable;
import com.dbn.database.interfaces.DatabaseInterface.Runnable;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

@UtilityClass
public final class Savepoints{

    public static <T> T call(@NotNull DBNResultSet resultSet, Callable<T> callable) throws SQLException {
        DBNConnection c = resultSet.getConnection();
        return call(c, callable);
    }

    public static <T> T call(DBNConnection conn, Callable<T> callable) throws SQLException {
        if (conn == null) {
            return callable.call();
        } else {
            return conn.withSavepoint(callable);
        }
    }

    public static void run(@NotNull DBNResultSet resultSet, Runnable runnable) throws SQLException {
        DBNConnection conn = resultSet.getConnection();
        run(conn, runnable);
    }

    public static void run(@Nullable DBNConnection conn, Runnable runnable) throws SQLException {
        if (conn == null) {
            runnable.run();
        } else {
            conn.withSavepoint(runnable);
        }
    }
}
