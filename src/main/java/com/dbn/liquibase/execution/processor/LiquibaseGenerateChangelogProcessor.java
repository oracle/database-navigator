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
import liquibase.change.core.TagDatabaseChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.serializer.ChangeLogSerializer;
import liquibase.serializer.ChangeLogSerializerFactory;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.snapshot.SnapshotListener;
import liquibase.structure.DatabaseObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.liquibase.execution.LiquibaseCommands.GENERATE_CHANGELOG;
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
public class LiquibaseGenerateChangelogProcessor extends LiquibaseExecutionProcessor {
    private static final String GENERATE_CHANGELOG_DIFF_TYPES =
            "catalogs,columns,foreignkeys,indexes,primarykeys,sequences,tables,uniqueconstraints,views";

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.GENERATE_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, false);
        prepareChangelogOutput(context);

        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        generateChangelog(context, paths, changelogFile, result);
        result.appendConsoleOutput(txt("log.liquibase.info.InitialChangelogGenerated", changelogFile));
    }

    private void generateChangelog(
        @NotNull LiquibaseExecutionContext context,
        @NotNull LiquibaseWorkspacePaths workspacePaths,
        @NotNull Path changelogFile,
        @NotNull LiquibaseExecutionResult result) throws Exception {
        Path contentRoot = workspacePaths.getContentRootPath();
        LiquibaseExecutionInput input = context.getInput();

        DBSchema sourceSchema = context.getSourceSchema();
        withLiquibaseDatabase(context, true, sourceSchema, database -> {
            checkCanceled(context);
            String schemaName = sourceSchema.getName();
            database.setDefaultSchemaName(schemaName);

            withLiquibaseScope(context, contentRoot, output -> {
                collectDatabaseObjects(context, database, schemaName, result);
                checkCanceled(context);

                executeCommand(GENERATE_CHANGELOG, output, Map.of(
                        "database", database,
                        "schemas", schemaName,
                        "diffTypes", GENERATE_CHANGELOG_DIFF_TYPES,
                        "changelogFile", changelogFile.toString(),
                        "excludeObjects", buildTrackingTableFilter(database),
                        "author", input.getChangelogAuthor()));

                String databaseTag = input.getDatabaseTag();
                if (isNotEmpty(databaseTag)) {
                    appendDatabaseTag(contentRoot, changelogFile, input.getChangelogAuthor(), databaseTag);
                }
                return null;
            });
            checkCanceled(context);

            if (!Files.isRegularFile(changelogFile)) {
                throw new IllegalStateException("Liquibase did not create the changelog file: " + changelogFile);
            }
            return null;
        });
    }

    private void appendDatabaseTag(
            @NotNull Path contentRoot,
            @NotNull Path changelogFile,
            @Nullable String author,
            @NotNull String tag) throws Exception {
        ResourceAccessor resourceAccessor = new DirectoryResourceAccessor(contentRoot);
        String changelogPath = contentRoot.relativize(changelogFile).toString().replace('\\', '/');
        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
                .getParser(changelogPath, resourceAccessor)
                .parse(changelogPath, new ChangeLogParameters(), resourceAccessor);

        ChangeSet changeSet = new ChangeSet(
                "baseline-tag-" + tag,
                isNotEmpty(author) ? author : "liquibase",
                false,
                false,
                changelogFile.toString(),
                null,
                null,
                changeLog);
        TagDatabaseChange tagChange = new TagDatabaseChange();
        tagChange.setTag(tag);
        changeSet.addChange(tagChange);
        changeLog.addChangeSet(changeSet);

        ChangeLogSerializer serializer = ChangeLogSerializerFactory.getInstance()
                .getSerializer(changelogPath);
        try (var output = Files.newOutputStream(
                changelogFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            serializer.write(changeLog.getChangeSets(), output);
        }
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
