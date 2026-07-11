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

import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseProcessedItem;
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
            if (Files.exists(changelogFile)) {
                throw new IllegalStateException("Changelog file already exists: " + changelogFile);
            }
            Files.createDirectories(changelogFile.getParent());
            generateChangelog(artifactPaths, changelogFile, result);

            result.appendConsoleOutput("Generated initial changelog: " + changelogFile + System.lineSeparator());
            result.finish(true);
        } catch (Exception e) {
            result.appendErrorOutput(formatException(e));
            result.finish(false);
        }
        return result;
    }

    private void generateChangelog(
            @NotNull LiquibaseArtifactPaths artifactPaths,
            @NotNull Path changelogFile,
            @NotNull LiquibaseExecutionResult result) throws Exception {
        Path contentRoot = artifactPaths.getContentRootPath();
        String relativeChangelogFile = artifactPaths.getRelativePath(changelogFile);

        withPoolConnection(true, c -> {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            String schemaName = getInput().getSchema().getName();
            database.setDefaultSchemaName(schemaName);
            collectDatabaseObjects(database, schemaName, result);
            Scope.child(Scope.Attr.resourceAccessor, new DirectoryResourceAccessor(contentRoot), () -> {
                new CommandScope("generate-changelog")
                        .addArgumentValue("database", database)
                        .addArgumentValue("schemas", schemaName)
                        .addArgumentValue("changelogFile", relativeChangelogFile)
                        .addArgumentValue("overwriteOutputFile", false)
                        .execute();
            });
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

                result.ensureProcessedItem(object);
            }

            @Override
            public void finishedSnapshot(DatabaseObject object, DatabaseObject snapshot, Database database) {
                if (snapshot == null) return;

                LiquibaseProcessedItem item = result.ensureProcessedItem(snapshot);
                result.updateProcessedItem(
                        item,
                        snapshot,
                        "discovered",
                        "Database object discovered during changelog generation");
            }
        });

        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(
                database.getDefaultCatalogName(),
                schemaName);
        SnapshotGeneratorFactory.getInstance().createSnapshot(catalogAndSchema, database, snapshotControl);
    }

    @NotNull
    private String formatException(@NotNull Exception exception) {
        StringWriter output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));
        return output.toString();
    }
}
