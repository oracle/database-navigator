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

import com.dbn.common.extension.ExtensionPoint;
import com.dbn.common.routine.ThrowableFunction;
import com.dbn.common.task.TaskStatus;
import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionLogService;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.object.DBSchema;
import com.intellij.openapi.extensions.ExtensionPointName;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CommandExecutionException;
import liquibase.resource.DirectoryResourceAccessor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

/**
 * Base class for state-independent processors that execute a single Liquibase operation.
 *
 * <p>A processor instance is registered as an extension-point prototype and receives the mutable
 * execution state through {@link LiquibaseExecutionContext}. This keeps operation-specific logic
 * reusable while allowing cancellation, logging, timing, and result publication to remain shared
 * across all Liquibase operations.</p>
 *
 * <p>The base implementation establishes the execution lifecycle, creates Liquibase database and
 * resource scopes, forwards Liquibase output to the DBN execution result, translates cancellation
 * and failures into {@link com.dbn.common.task.TaskStatus} values, and provides the common database
 * connection helpers used by specialized processors.</p>
 */
public abstract class LiquibaseExecutionProcessor implements ExtensionPoint {
    public static final ExtensionPointName<LiquibaseExecutionProcessor> EP =
            ExtensionPointName.create("com.dbn.liquibaseExecutionProcessor");
    protected LiquibaseExecutionProcessor() {
    }

    public abstract LiquibaseOperation getOperation();

    @NotNull
    public final LiquibaseExecutionResult execute(@NotNull LiquibaseExecutionContext context) {
        LiquibaseExecutionResult result = context.prepareExecutionResult();
        context.setExecutionThread(Thread.currentThread());
        result.notifyStarted();
        try {
            executeOperation(context);
            finishResult(context, TaskStatus.DONE);
        } catch (CancellationException e) {
            finishResult(context, TaskStatus.CANCELLED);
        } catch (Exception e) {
            result.appendErrorOutput(formatException(e));
            finishResult(context, TaskStatus.FAILED);
        }
        return result;
    }

    protected abstract void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception;

    protected final void checkCanceled(@NotNull LiquibaseExecutionContext context) {
        if (context.isCancellationRequested()) throw new CancellationException("Liquibase execution canceled");
    }

    @NotNull
    protected final String formatException(@NotNull Exception exception) {
        StringWriter output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    protected final <T> T withLiquibaseDatabase(
            @NotNull LiquibaseExecutionContext context,
            boolean readonly,
            @NotNull DBSchema schema,
            @NotNull ThrowableFunction<Database, T, Exception> operation) throws SQLException {
        ConnectionHandler connection = schema.getConnection();

        return withPoolConnection(context, readonly, connection, schema.getSchemaId(), c -> {
            Connection dbConnection = DBNConnection.getInner(c);
            DatabaseCompatibilityInterface compatibilityInterface = connection.getCompatibilityInterface();
            compatibilityInterface.initializeLiquibaseConnection(dbConnection);

            DatabaseFactory databaseFactory = DatabaseFactory.getInstance();
            JdbcConnection jdbcConnection = new JdbcConnection(dbConnection);
            Database database = databaseFactory.findCorrectDatabaseImplementation(jdbcConnection);
            String catalogName = compatibilityInterface.getLiquibaseCatalogName(schema.getName());
            if (catalogName != null) database.setDefaultCatalogName(catalogName);
            database.setDefaultSchemaName(schema.getName());
            context.getResult().setLiquibaseTableNames(
                    database.getDatabaseChangeLogTableName(),
                    database.getDatabaseChangeLogLockTableName());

            return operation.apply(database);
        });
    }

    protected final <T> T withLiquibaseScope(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Path contentRoot,
            @NotNull ThrowableFunction<LiquibaseExecutionOutputStream, T, Exception> operation) throws Exception {
        LiquibaseExecutionResult result = context.getResult();
        Map<String, Object> scopeValues = Map.of(
                Scope.Attr.logService.name(), new LiquibaseExecutionLogService(result),
                Scope.Attr.resourceAccessor.name(), new DirectoryResourceAccessor(contentRoot));
        return child(scopeValues, () -> {
            try (LiquibaseExecutionOutputStream output = new LiquibaseExecutionOutputStream(result)) {
                return operation.apply(output);
            }
        });
    }

    protected final void finishResult(@NotNull LiquibaseExecutionContext context, @NotNull TaskStatus status) {
        LiquibaseExecutionResult result = context.getResult();
        if (context.isCancellationRequested()) {
            result.notifyCancelled();
        } else {
            result.notifyFinished(status);
        }
        context.clearExecutionThread();
    }

    private <T> T withPoolConnection(
            @NotNull LiquibaseExecutionContext context,
            boolean readonly,
            @NotNull ConnectionHandler connection,
            @Nullable SchemaId schemaId,
            @NotNull ThrowableFunction<DBNConnection, T, Exception> operation) throws SQLException {
        checkCanceled(context);
        ConnectionContext connectionContext = new ConnectionContext(
                connection.getProject(),
                connection.getConnectionId(),
                schemaId);
        return PooledConnection.call(connectionContext, readonly, c ->
                withClassLoader(LiquibaseExecutionProcessor.class, () -> {
                    try {
                        checkCanceled(context);
                        T result = operation.apply(c);
                        checkCanceled(context);
                        return result;
                    } catch (Throwable e) {
                        if (e instanceof CancellationException cancellationException) throw cancellationException;
                        throw toSqlException(unwrap(e));
                    }
                }));
    }

    protected static Object executeCommand(String commandName, LiquibaseExecutionOutputStream output, @NonNls Map<String, Object> arguments) throws CommandExecutionException {
        CommandScope command = new CommandScope(commandName);

        for (String argument : arguments.keySet()) {
            Object value = arguments.get(argument);
            if (value == null) continue;

            command.addArgumentValue(argument, value);
        }

        command.setOutput(output);
        command.execute();

        return null;
    }

    @NotNull
    public static <T extends DatabaseEntity> T required(@NonNls String name, @Nullable T entity) {
        if (entity == null) throw new IllegalStateException(name + " not specified");
        return entity;
    }

}
