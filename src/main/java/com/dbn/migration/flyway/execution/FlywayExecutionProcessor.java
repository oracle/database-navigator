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

package com.dbn.migration.flyway.execution;

import com.dbn.common.exception.RequestCancelledException;
import com.dbn.common.extension.ExtensionPoint;
import com.dbn.common.task.TaskStatus;
import com.dbn.connection.ConnectionHandler;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.operation.FlywayOperationContext;
import com.dbn.migration.flyway.operation.FlywayOperationInput;
import com.dbn.migration.flyway.operation.FlywayOperationResult;
import com.dbn.migration.flyway.workspace.FlywayWorkspacePaths;
import com.dbn.migration.shared.execution.DatabaseMigrationOperationExecutor;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Base processor for Flyway operations.
 *
 * <p>The base implementation owns the common execution lifecycle, cancellation handling,
 * workspace resolution, Flyway configuration, and failure publication. Concrete processors
 * provide the operation identity and operation-specific work.</p>
 */
public abstract class FlywayExecutionProcessor implements ExtensionPoint, DatabaseMigrationOperationExecutor<
        FlywayOperation,
        FlywayOperationInput,
        FlywayOperationContext,
        FlywayOperationResult> {
    public static final ExtensionPointName<FlywayExecutionProcessor> EP =
            ExtensionPointName.create("com.dbn.flywayExecutionProcessor");

    protected FlywayExecutionProcessor() {
    }

    @Override
    public abstract @NotNull FlywayOperation getOperation();

    @NotNull
    @Override
    public final FlywayOperationResult execute(@NotNull FlywayOperationContext context) {
        FlywayOperationResult result = context.getResult();
        context.setExecutionThread(Thread.currentThread());
        result.notifyStarted();
        try {
            checkCancelled(context);
            FlywayWorkspacePaths paths = context.getInput().getWorkspacePaths();
            if (paths == null) throw new IllegalStateException("No Flyway workspace selected");

            TaskStatus status = executeOperation(context, paths);
            result.notifyFinished(status);
        } catch (RequestCancelledException e) {
            result.notifyCancelled();
        } catch (Exception e) {
            result.appendErrorOutput(formatException(e));
            result.notifyFinished(TaskStatus.FAILED);
        } finally {
            context.clearExecutionThread();
        }
        return result;
    }

    /**
     * Executes the operation-specific Flyway call and returns its final task status.
     */
    @NotNull
    protected abstract TaskStatus executeOperation(
            @NotNull FlywayOperationContext context,
            @NotNull FlywayWorkspacePaths paths) throws Exception;

    @NotNull
    protected final Flyway createFlyway(
            @NotNull FlywayOperationContext context,
            @NotNull FlywayWorkspacePaths paths) {
        ConnectionHandler connection = context.getSchema().getConnection();
        Path migrationsPath = paths.getMigrationsPath();
        String schemaName = context.getSchema().getName();

        return Flyway.configure()
                .workingDirectory(paths.getFlywayRootPath().toString())
                .loadDefaultConfigurationFiles()
                .dataSource(new FlywayConnectionDataSource(connection, context.getSchema().getSchemaId()))
                .locations("filesystem:" + migrationsPath)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .load();
    }

    private static void checkCancelled(@NotNull FlywayOperationContext context) {
        if (context.isCancellationRequested()) {
            throw new RequestCancelledException("Flyway execution canceled");
        }
    }

    @NotNull
    private static String formatException(@NotNull Exception exception) {
        StringBuilder output = new StringBuilder(exception.toString());
        for (StackTraceElement element : exception.getStackTrace()) {
            output.append("\n\tat ").append(element);
        }
        return output.toString();
    }
}
