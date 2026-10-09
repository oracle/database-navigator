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

package com.dbn.migration.flyway.execution.processor;

import com.dbn.common.task.TaskStatus;
import com.dbn.connection.DatabaseType;
import com.dbn.migration.flyway.execution.FlywayExecutionProcessor;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.operation.FlywayOperationContext;
import com.dbn.migration.flyway.operation.FlywayOperationResult;
import com.dbn.migration.flyway.workspace.FlywayWorkspacePaths;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static com.dbn.nls.NlsResources.txt;

/**
 * Creates the initial Flyway project structure in the selected workspace.
 *
 * <p>The Flyway Java API does not expose the command-line {@code init} operation. This processor
 * therefore creates the workspace directories and the standard TOML configuration files while
 * preserving files that already exist.</p>
 */
public final class FlywayInitializeProjectProcessor extends FlywayExecutionProcessor {
    @NonNls
    private static final String CONFIGURATION_TEMPLATE = """
            databaseType = "%s"

            [flyway]
            locations = ["filesystem:%s"]
            """;

    @Override
    public @NotNull FlywayOperation getOperation() {
        return FlywayOperation.INITIALIZE_PROJECT;
    }

    @Override
    protected @NotNull TaskStatus executeOperation(
            @NotNull FlywayOperationContext context,
            @NotNull FlywayWorkspacePaths paths) throws IOException {
        Path rootPath = paths.getFlywayRootPath();
        Path migrationsPath = paths.getMigrationsPath();
        DatabaseType databaseType = context.getSchema().getConnection().getDatabaseType();
        String configuration = CONFIGURATION_TEMPLATE.formatted(
                getFlywayDatabaseType(databaseType),
                paths.getWorkspace().getMigrationsDirectory().replace('\\', '/'));

        Files.createDirectories(rootPath);
        Files.createDirectories(migrationsPath);
        createFile(paths.getConfigurationFilePath(), configuration);
        createFile(paths.getUserConfigurationFilePath(), "");

        FlywayOperationResult result = context.getResult();
        result.appendConsoleOutput(txt("log.flyway.info.ProjectInitialized", rootPath));
        return TaskStatus.DONE;
    }

    @NotNull
    private static String getFlywayDatabaseType(@NotNull DatabaseType databaseType) {
        return switch (databaseType) {
            case ORACLE -> "oracle";
            case MYSQL -> "mysql";
            case POSTGRES -> "postgresql";
            default -> throw new UnsupportedOperationException(
                    "Flyway project initialization is not supported for database type: " + databaseType);
        };
    }

    private static void createFile(@NotNull Path path, @NotNull String content) throws IOException {
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
        try {
            Files.writeString(
                    path,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException ignored) {
            if (!Files.isRegularFile(path)) throw ignored;
        }
    }
}
