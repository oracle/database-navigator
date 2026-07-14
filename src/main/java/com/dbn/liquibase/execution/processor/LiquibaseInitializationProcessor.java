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

import com.dbn.common.util.Strings;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionItem;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import liquibase.CatalogAndSchema;
import liquibase.command.CommandScope;
import liquibase.database.Database;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.snapshot.SnapshotListener;
import liquibase.structure.DatabaseObject;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.resolveObjectType;
import static com.dbn.nls.NlsResources.txt;

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

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionResult result) throws Exception {
        LiquibaseWorkspacePaths paths = getInput().getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        result.setChangelogPath(changelogFile);
        if (Files.exists(changelogFile)) {
            throw new IllegalStateException("Changelog file already exists: " + changelogFile);
        }
        Files.createDirectories(changelogFile.getParent());
        generateChangelog(paths, changelogFile, result);
        result.appendConsoleOutput(txt("log.liquibase.info.InitialChangelogGenerated", changelogFile));
    }

    private void generateChangelog(
        @NotNull LiquibaseWorkspacePaths workspacePaths,
        @NotNull Path changelogFile,
        @NotNull LiquibaseExecutionResult result) throws Exception {
        Path contentRoot = workspacePaths.getContentRootPath();

        DBSchema sourceSchema = required("Source schema", getInput().getSourceSchema());
        withLiquibaseDatabase(true, sourceSchema, database -> {
            checkCanceled();
            String schemaName = sourceSchema.getName();
            database.setDefaultSchemaName(schemaName);

            withLiquibaseScope(contentRoot, result, output -> {
                collectDatabaseObjects(database, schemaName, result);
                checkCanceled();
                new CommandScope("generateChangelog")
                        .addArgumentValue("database", database)
                        .addArgumentValue("schemas", schemaName)
                        .addArgumentValue("changelogFile", changelogFile.toString())
                        .addArgumentValue("overwriteOutputFile", false)
                        .setOutput(output)
                        .execute();
                return null;
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

                result.appendInfoOutput(txt(
                        "log.liquibase.info.ObjectProcessingStarted",
                        describe(item)));
            }

            @Override
            public void finishedSnapshot(DatabaseObject object, DatabaseObject snapshot, Database database) {
                if (object == null) return;

                checkCanceled();
                LiquibaseExecutionItem item = result.ensureExecutionItem(object);
                item.finishProcessing();

                DatabaseObject processedObject = snapshot == null ? object : snapshot;
                result.appendInfoOutput(txt(
                        "log.liquibase.info.ObjectProcessingFinished",
                        describe(item),
                        presentableDuration(item.getProcessingDuration(), true)));
                result.updateExecutionItem(
                        item,
                        processedObject,
                        LiquibaseExecutionItemStatus.DISCOVERED,
                        txt("log.liquibase.info.DatabaseObjectDiscovered"));
            }
        });

        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(
                database.getDefaultCatalogName(),
                schemaName);
        SnapshotGeneratorFactory.getInstance().createSnapshot(catalogAndSchema, database, snapshotControl);
    }

    @NotNull
    private static String describe(@NotNull DBObjectType objectType, String objectName) {
        String name = Strings.isEmpty(objectName) ? txt("app.shared.placeholder.Unnamed") : objectName;
        return objectType.getDisplayName() + " \"" + name + "\"";
    }

    @NotNull
    private static String describe(@NotNull LiquibaseExecutionItem item) {
        DatabaseObject containerObject = item.getContainerObject();
        String description = describe(item.getObjectType(), item.getDatabaseObject().getName());
        if (containerObject == null) return description;

        String parentName = containerObject.getName();
        DBObjectType parentType = resolveObjectType(containerObject);
        String parentDesc = describe(parentType, parentName);

        return txt("log.liquibase.info.ObjectInContainer", description, parentDesc);
    }

}
