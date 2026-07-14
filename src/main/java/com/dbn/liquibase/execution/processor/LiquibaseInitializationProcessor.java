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

import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseSnapshotItem;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.snapshot.SnapshotListener;
import liquibase.structure.DatabaseObject;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.buildTrackingTableFilter;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.isLiquibaseTrackingObject;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.resolveObjectType;
import static com.dbn.nls.NlsResources.txt;

/**
 * Generates the baseline Liquibase changelog for the source schema selected in the execution input.
 *
 * <p>The processor opens the source database in read-only mode and asks Liquibase to snapshot the
 * supported schema objects, including tables, columns, constraints, indexes, sequences, and views.
 * Liquibase's own tracking tables and related objects are excluded from the generated model because
 * they describe Liquibase bookkeeping rather than the application schema.</p>
 *
 * <p>The resulting changelog is written to the workspace master changelog path. If that file already
 * exists, the user must explicitly approve overwriting it. Snapshot items are added to the execution
 * result while the snapshot is being collected so the result form can display the operation's progress.</p>
 */
public class LiquibaseInitializationProcessor extends LiquibaseExecutionProcessor {
    private static final String GENERATE_CHANGELOG_DIFF_TYPES =
            "catalogs,columns,foreignkeys,indexes,primarykeys,sequences,tables,uniqueconstraints,views";

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.INITIALIZE;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        result.setChangelogPath(changelogFile);
        boolean overwrite = false;
        if (Files.exists(changelogFile)) {
            int option = Messages.showConfirmationDialog(
                    input.getProject(),
                    txt("msg.liquibase.title.OverwriteChangelog"),
                    txt("msg.liquibase.question.OverwriteChangelog", changelogFile),
                    Messages.options(
                            txt("msg.liquibase.button.Overwrite"),
                            txt("msg.shared.button.Cancel")),
                    0);
            if (option != 0) throw new CancellationException("Changelog overwrite canceled");
            overwrite = true;
        }
        Files.createDirectories(changelogFile.getParent());
        generateChangelog(context, paths, changelogFile, result, overwrite);
        result.appendConsoleOutput(txt("log.liquibase.info.InitialChangelogGenerated", changelogFile));
    }

    private void generateChangelog(
        @NotNull LiquibaseExecutionContext context,
        @NotNull LiquibaseWorkspacePaths workspacePaths,
        @NotNull Path changelogFile,
        @NotNull LiquibaseExecutionResult result,
        boolean overwrite) throws Exception {
        Path contentRoot = workspacePaths.getContentRootPath();

        DBSchema sourceSchema = required("Source schema", context.getInput().getSourceSchema());
        withLiquibaseDatabase(context, true, sourceSchema, database -> {
            checkCanceled(context);
            String schemaName = sourceSchema.getName();
            database.setDefaultSchemaName(schemaName);

            withLiquibaseScope(context, contentRoot, output -> {
                collectDatabaseObjects(context, database, schemaName, result);
                checkCanceled(context);
                return executeCommand("generateChangelog", output, Map.of(
                        "database", database,
                        "schemas", schemaName,
                        "diffTypes", GENERATE_CHANGELOG_DIFF_TYPES,
                        "changelogFile", changelogFile.toString(),
                        "overwriteOutputFile", overwrite,
                        "excludeObjects", buildTrackingTableFilter(database)));
            });
            checkCanceled(context);

            if (!Files.isRegularFile(changelogFile)) {
                throw new IllegalStateException("Liquibase did not create the changelog file: " + changelogFile);
            }
            return null;
        });
    }

    private void collectDatabaseObjects(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull String schemaName,
        @NotNull LiquibaseExecutionResult result) throws Exception {
        SnapshotControl snapshotControl = new SnapshotControl(database);
        snapshotControl.setSnapshotListener(new SnapshotListener() {
            @Override
            public void willSnapshot(DatabaseObject object, Database database) {
                if (object == null || isLiquibaseTrackingObject(object, database)) return;

                checkCanceled(context);
                LiquibaseSnapshotItem item = result.ensureSnapshotItem(object);
                item.startProcessing();

                result.appendInfoOutput(txt(
                        "log.liquibase.info.ObjectProcessingStarted",
                        describe(item)));
            }

            @Override
            public void finishedSnapshot(DatabaseObject object, DatabaseObject snapshot, Database database) {
                if (object == null) return;
                if (isLiquibaseTrackingObject(object, database)) return;

                checkCanceled(context);
                LiquibaseSnapshotItem item = result.ensureSnapshotItem(object);
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
    private static String describe(@NotNull LiquibaseSnapshotItem item) {
        DatabaseObject containerObject = item.getContainerObject();
        String description = describe(item.getObjectType(), item.getDatabaseObject().getName());
        if (containerObject == null) return description;

        String parentName = containerObject.getName();
        DBObjectType parentType = resolveObjectType(containerObject);
        String parentDesc = describe(parentType, parentName);

        return txt("log.liquibase.info.ObjectInContainer", description, parentDesc);
    }

}
