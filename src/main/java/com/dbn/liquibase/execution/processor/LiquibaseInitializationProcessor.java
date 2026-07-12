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

package com.dbn.liquibase.execution.processor;

import com.dbn.common.task.TaskStatus;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionItem;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionLogService;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.model.LiquibaseArtifactPaths;
import liquibase.CatalogAndSchema;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.snapshot.SnapshotListener;
import liquibase.structure.DatabaseObject;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.liquibase.execution.logging.LiquibaseExecutionLogging.isLoggableObject;
import static com.dbn.nls.NlsResources.txt;
import static liquibase.Scope.child;

/**
 * Processor for generating an initial changelog from a database schema.
 */
public class LiquibaseInitializationProcessor extends LiquibaseExecutionProcessor {
    public LiquibaseInitializationProcessor(@NotNull LiquibaseExecutionInput input) {
        super(input);
    }

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.INITIALIZE;
    }

    @NotNull
    @Override
    public LiquibaseExecutionResult execute() {
        LiquibaseExecutionResult result = super.execute();
        try {
            LiquibaseArtifactPaths artifactPaths = getInput().getArtifactPaths();
            Path changelogFile = artifactPaths.getMasterChangelogPath();
            result.setChangelogPath(changelogFile);
            if (Files.exists(changelogFile)) {
                throw new IllegalStateException("Changelog file already exists: " + changelogFile);
            }
            Files.createDirectories(changelogFile.getParent());
            generateChangelog(artifactPaths, changelogFile, result);

            result.appendConsoleOutput(txt("log.liquibase.info.InitialChangelogGenerated", changelogFile));
            finishResult(TaskStatus.DONE);
        } catch (CancellationException e) {
            finishResult(TaskStatus.CANCELLED);
        } catch (Exception e) {
            result.appendErrorOutput(formatException(e));
            finishResult(TaskStatus.FAILED);
        }
        return result;
    }

    private void generateChangelog(
        @NotNull LiquibaseArtifactPaths artifactPaths,
        @NotNull Path changelogFile,
        @NotNull LiquibaseExecutionResult result) throws Exception {
        Path contentRoot = artifactPaths.getContentRootPath();

        withPoolConnection(true, c -> {
            checkCanceled();
            DatabaseFactory databaseFactory = DatabaseFactory.getInstance();
            JdbcConnection connection = new JdbcConnection(DBNConnection.getInner(c));
            Database database = databaseFactory.findCorrectDatabaseImplementation(connection);

            String schemaName = getInput().getSchema().getName();
            database.setDefaultSchemaName(schemaName);

            LiquibaseExecutionLogService logService = new LiquibaseExecutionLogService(result);
            child(Scope.Attr.logService, logService, () -> {
                collectDatabaseObjects(database, schemaName, result);
                checkCanceled();
                child(Scope.Attr.resourceAccessor, new DirectoryResourceAccessor(contentRoot), () -> {
                    checkCanceled();
                    try (LiquibaseExecutionOutputStream output = new LiquibaseExecutionOutputStream(result)) {
                        new CommandScope("generateChangelog")
                                .addArgumentValue("database", database)
                                .addArgumentValue("schemas", schemaName)
                                .addArgumentValue("changelogFile", changelogFile.toString())
                                .addArgumentValue("overwriteOutputFile", false)
                                .setOutput(output)
                                .execute();
                    }
                });
            });
            checkCanceled();

            if (!Files.isRegularFile(changelogFile)) {
                throw new IllegalStateException("Liquibase did not create the changelog file: " + changelogFile);
            }
            return null;
        });
    }

    private void collectDatabaseObjects(
            @NotNull Database database,
            @NotNull String schemaName,
        @NotNull LiquibaseExecutionResult result) throws Exception {
        SnapshotControl snapshotControl = new SnapshotControl(database);
        snapshotControl.setSnapshotListener(new SnapshotListener() {
            @Override
            public void willSnapshot(DatabaseObject object, Database database) {
                if (object == null) return;
                checkCanceled();
                LiquibaseExecutionItem item = result.ensureExecutionItem(object);
                item.startProcessing();

                if (isLoggableObject(object)) {
                    result.appendInfoOutput(txt("log.liquibase.info.ObjectProcessingStarted", describe(object)));
                }
            }

            @Override
            public void finishedSnapshot(DatabaseObject object, DatabaseObject snapshot, Database database) {
                if (snapshot == null) return;
                checkCanceled();
                LiquibaseExecutionItem item = result.ensureExecutionItem(snapshot);
                item.finishProcessing();

                if (isLoggableObject(snapshot)) {
                    result.appendInfoOutput(txt(
                            "log.liquibase.info.ObjectProcessingFinished",
                            describe(snapshot),
                            presentableDuration(item.getProcessingDuration(), true)));
                }
                result.updateExecutionItem(
                        item,
                        snapshot,
                        "discovered",
                        txt("log.liquibase.info.DatabaseObjectDiscovered"));
            }
        });

        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(
                database.getDefaultCatalogName(),
                schemaName);
        SnapshotGeneratorFactory.getInstance().createSnapshot(catalogAndSchema, database, snapshotControl);
    }

    @NotNull
    private static String describe(@NotNull DatabaseObject object) {
        return object.getObjectTypeName() + " \"" + object.getName() + "\"";
    }

    @NotNull
    private String formatException(@NotNull Exception exception) {
        StringWriter output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));
        return output.toString();
    }
}
