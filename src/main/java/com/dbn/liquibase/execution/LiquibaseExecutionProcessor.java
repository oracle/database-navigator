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
import com.dbn.common.task.TaskStatus;
import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionLogService;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.exception.Exceptions.unwrap;
import static com.dbn.common.util.Classes.withClassLoader;
import static liquibase.Scope.child;

/** Coordinates execution of a Liquibase input and publishes its execution result. */
@Getter
public abstract class LiquibaseExecutionProcessor {
    private final LiquibaseExecutionInput input;
    private LiquibaseExecutionResult result;
    private volatile Thread executionThread;
    private volatile boolean cancellationRequested;

    public LiquibaseExecutionProcessor(@NotNull LiquibaseExecutionInput input) {
        this.input = input;
        if (input.getOperation() != getOperation()) {
            throw new IllegalArgumentException("Invalid operation for Liquibase processor");
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
    public final LiquibaseExecutionResult execute() {
        LiquibaseExecutionResult result = prepareExecutionResult();
        executionThread = Thread.currentThread();
        result.notifyStarted();
        try {
            executeOperation(result);
            finishResult(TaskStatus.DONE);
        } catch (CancellationException e) {
            finishResult(TaskStatus.CANCELLED);
        } catch (Exception e) {
            result.appendErrorOutput(formatException(e));
            finishResult(TaskStatus.FAILED);
        }
        return result;
    }

    protected abstract void executeOperation(@NotNull LiquibaseExecutionResult result) throws Exception;

    public void cancel() {
        cancellationRequested = true;
        Thread thread = executionThread;
        if (thread != null) thread.interrupt();
    }

    protected final boolean isCancellationRequested() {
        return cancellationRequested || Thread.currentThread().isInterrupted();
    }

    protected final void checkCanceled() {
        if (isCancellationRequested()) throw new CancellationException("Liquibase execution canceled");
    }

    @NotNull
    protected final String formatException(@NotNull Exception exception) {
        StringWriter output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    protected final <T> T withLiquibaseDatabase(
            boolean readonly,
            @NotNull ThrowableFunction<Database, T, Exception> operation) throws SQLException {
        return withPoolConnection(readonly, c -> {
            Connection connection = DBNConnection.getInner(c);
            DatabaseCompatibilityInterface compatibilityInterface = input.getConnection().getCompatibilityInterface();
            compatibilityInterface.initializeLiquibaseConnection(connection);

            DatabaseFactory databaseFactory = DatabaseFactory.getInstance();
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = databaseFactory.findCorrectDatabaseImplementation(jdbcConnection);
            database.setDefaultSchemaName(input.getSchema().getName());

            return operation.apply(database);
        });
    }

    protected final <T> T withLiquibaseScope(
            @NotNull Path contentRoot,
            @NotNull LiquibaseExecutionResult result,
            @NotNull ThrowableFunction<LiquibaseExecutionOutputStream, T, Exception> operation) throws Exception {
        Map<String, Object> scopeValues = Map.of(
                Scope.Attr.logService.name(), new LiquibaseExecutionLogService(result),
                Scope.Attr.resourceAccessor.name(), new DirectoryResourceAccessor(contentRoot));
        return child(scopeValues, () -> {
            try (LiquibaseExecutionOutputStream output = new LiquibaseExecutionOutputStream(result)) {
                return operation.apply(output);
            }
        });
    }

    protected final void finishResult(@NotNull TaskStatus status) {
        LiquibaseExecutionResult result = prepareExecutionResult();
        if (cancellationRequested) {
            result.notifyCancelled();
        } else {
            result.notifyFinished(status);
        }
        executionThread = null;
    }

    protected <T> T withPoolConnection(boolean readonly, @NotNull ThrowableFunction<DBNConnection, T, Exception> operation) throws SQLException {
        checkCanceled();
        ConnectionHandler connection = input.getConnection();
        ConnectionContext context = new ConnectionContext(
                connection.getProject(),
                connection.getConnectionId(),
                null);
        return PooledConnection.call(context, readonly, c ->
                withClassLoader(LiquibaseExecutionProcessor.class, () -> {
                    try {
                        checkCanceled();
                        T result = operation.apply(c);
                        checkCanceled();
                        return result;
                    } catch (Throwable e) {
                        if (e instanceof CancellationException cancellationException) throw cancellationException;
                        throw toSqlException(unwrap(e));
                    }
                }));
    }

}
