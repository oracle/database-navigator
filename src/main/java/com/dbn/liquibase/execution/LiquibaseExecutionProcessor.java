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

package com.dbn.liquibase.execution;

import com.dbn.common.routine.ThrowableFunction;
import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.jdbc.DBNConnection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.exception.Exceptions.unwrap;
import static com.dbn.common.util.Classes.withClassLoader;

/** Coordinates execution of a Liquibase input and publishes its execution result. */
@Getter
public abstract class LiquibaseExecutionProcessor {
    private final LiquibaseExecutionInput input;
    private LiquibaseExecutionResult result;

    public LiquibaseExecutionProcessor(@NotNull LiquibaseExecutionInput input) {
        this.input = input;
        if (input.getOperation() != getOperation()) {
            throw new IllegalArgumentException("Invalid operation for initial changelog processor");
        }
    }

    public abstract LiquibaseOperation getOperation();

    @NotNull
    public LiquibaseExecutionResult prepareExecutionResult() {
        if (result == null) {
            result = new LiquibaseExecutionResult(
                    input.getSchema(),
                    input.getOperation());
        }
        return result;
    }

    @NotNull
    public LiquibaseExecutionResult execute() {
        LiquibaseExecutionResult result = prepareExecutionResult();
        result.start();
        return result;
    }

    protected <T> T withPoolConnection(boolean readonly, @NotNull ThrowableFunction<DBNConnection, T, Exception> operation) throws SQLException {
        ConnectionHandler connection = input.getConnection();
        ConnectionContext context = new ConnectionContext(
                connection.getProject(),
                connection.getConnectionId(),
                null);
        return PooledConnection.call(context, readonly, c ->
                withClassLoader(LiquibaseExecutionProcessor.class, () -> {
                    try {
                        return operation.apply(c);
                    } catch (Throwable e) {
                        throw toSqlException(unwrap(e));
                    }
                }));
    }

}
