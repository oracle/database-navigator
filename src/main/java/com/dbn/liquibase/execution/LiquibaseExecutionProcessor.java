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

import com.dbn.common.exception.ElementSkippedException;
import com.dbn.common.exception.RequestCancelledException;
import com.dbn.common.extension.ExtensionPoint;
import com.dbn.common.routine.ThrowableConsumer;
import com.dbn.common.task.TaskStatus;
import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionLogService;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.extensions.ExtensionPointName;
import liquibase.CatalogAndSchema;
import liquibase.Scope;
import liquibase.change.core.TagDatabaseChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.command.CommandResults;
import liquibase.command.CommandScope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CommandExecutionException;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.serializer.ChangeLogSerializer;
import liquibase.serializer.ChangeLogSerializerFactory;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.snapshot.SnapshotListener;
import liquibase.structure.DatabaseObject;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.exception.Exceptions.unwrap;
import static com.dbn.common.util.Classes.withClassLoader;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.isLiquibaseTrackingObject;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.resolveObjectType;
import static com.dbn.liquibase.execution.LiquibaseOperationConfirmations.ensureConfirmed;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.UNSPECIFIED;
import static com.dbn.object.type.DBObjectType.BROWSABLE_TYPES;
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

    protected static void rememberTag(@NotNull LiquibaseExecutionContext context, DBSchema targetSchema, String tag) {
        DatabaseLiquibaseManager liquibaseManager = context.getLiquibaseManager();
        liquibaseManager.rememberTag(
                targetSchema.getConnectionId(),
                targetSchema.getSchemaId(),
                tag);
    }

    protected static @NotNull DirectoryResourceAccessor contentRootAccessor(LiquibaseExecutionContext context) throws FileNotFoundException {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        return new DirectoryResourceAccessor(paths.getContentRootPath());
    }

    protected static @NotNull ClassLoaderResourceAccessor classLoaderAccessor() {
        return new ClassLoaderResourceAccessor(LiquibaseExecutionProcessor.class.getClassLoader());
    }

    protected static void notifySchemaObjectChanges(@NotNull DBSchema schema) {
        BROWSABLE_TYPES.stream()
                .filter(type -> type.isSchemaObject())
                .forEach(type -> ObjectChangeEvent.notify(
                        UNSPECIFIED,
                        type,
                        schema.getConnectionId(),
                        schema.getSchemaId()));
    }

    @NotNull
    protected final List<ChangeSet> discoverPendingChangeSets(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        ResourceAccessor resourceAccessor = contentRootAccessor(context);
        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
                .getParser(paths.getMasterChangelogRelativePath(), resourceAccessor)
                .parse(paths.getMasterChangelogRelativePath(), new ChangeLogParameters(), resourceAccessor);

        List<ChangeSet> changeSets = new ArrayList<>();
        for (ChangeSet changeSet : changeLog.getChangeSets()) {
            if (database.getRunStatus(changeSet) == ChangeSet.RunStatus.NOT_RAN) {
                changeSets.add(changeSet);
            }
        }
        return changeSets;
    }

    @NotNull
    protected final List<LiquibaseChangeSetItem> discoverChangeSetItems(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull Function<ChangeSet, String> messageFunction) throws Exception {
        List<LiquibaseChangeSetItem> items = new ArrayList<>();
        for (ChangeSet changeSet : discoverPendingChangeSets(context, database)) {
            items.add(context.getResult().ensureChangeSetItem(
                    changeSet,
                    LiquibaseExecutionItemStatus.DISCOVERED,
                    messageFunction.apply(changeSet)));
        }
        return items;
    }

    protected final void completeChangeSetItems(
            @NotNull LiquibaseExecutionContext context,
            @NotNull List<LiquibaseChangeSetItem> items,
            @NotNull Function<LiquibaseChangeSetItem, String> messageFunction) {
        LiquibaseExecutionResult result = context.getResult();
        for (LiquibaseChangeSetItem item : items) {
            item.updateStatus(LiquibaseExecutionItemStatus.PROCESSED, messageFunction.apply(item));
        }
        if (!items.isEmpty()) result.notifyItemsChanged();
    }

    protected final void collectDatabaseObjects(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull String schemaName) throws Exception {
        LiquibaseExecutionResult result = context.getResult();
        SnapshotControl snapshotControl = new SnapshotControl(database);
        snapshotControl.setSnapshotListener(new SnapshotListener() {
            @Override
            public void willSnapshot(DatabaseObject object, Database database) {
                if (object == null || isLiquibaseTrackingObject(object, database)) return;

                checkCanceled(context);
                LiquibaseSnapshotItem item = result.ensureSnapshotItem(object);
                item.startProcessing();
                result.appendInfoOutput(txt("log.liquibase.info.ObjectProcessingStarted", describe(item)));
            }

            @Override
            public void finishedSnapshot(DatabaseObject object, DatabaseObject snapshot, Database database) {
                if (object == null || isLiquibaseTrackingObject(object, database)) return;

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

        SnapshotGeneratorFactory.getInstance().createSnapshot(
                new CatalogAndSchema(database.getDefaultCatalogName(), schemaName),
                database,
                snapshotControl);
    }

    @NotNull
    private static String describe(@NotNull DBObjectType objectType, String objectName) {
        String name = isEmpty(objectName) ? txt("app.shared.placeholder.Unnamed") : objectName;
        return objectType.getDisplayName() + " \"" + name + "\"";
    }

    @NotNull
    private static String describe(@NotNull LiquibaseSnapshotItem item) {
        DatabaseObject containerObject = item.getContainerObject();
        String description = describe(item.getObjectType(), item.getDatabaseObject().getName());
        if (containerObject == null) return description;

        return txt("log.liquibase.info.ObjectInContainer", description,
                describe(resolveObjectType(containerObject), containerObject.getName()));
    }

    protected static @NotNull Consumer<String> sqlOutputBuilder(@NotNull LiquibaseExecutionContext context) {
        return sql -> context.getResult().appendSqlOutput(sql);
    }

    protected final void appendDatabaseTag(
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
        try (OutputStream output = Files.newOutputStream(
                changelogFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            serializer.write(changeLog.getChangeSets(), output);
        }
    }

    protected final void prepareChangelogOutput(@NotNull LiquibaseExecutionContext context) throws Exception {
        LiquibaseExecutionInput input = context.getInput();
        Path changelogFile = input.getWorkspacePaths().getMasterChangelogPath();
        boolean overwrite = input.isConfirmed();
        if (overwrite) Files.deleteIfExists(changelogFile);
        Files.createDirectories(changelogFile.getParent());
    }

    protected final void prepareChangelogContext(
            @NotNull LiquibaseExecutionContext context,
            boolean requireExistingChangelog) {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        context.getResult().setChangelogPath(changelogFile);
        if (requireExistingChangelog && !Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Changelog file does not exist: " + changelogFile);
        }
    }

    public abstract LiquibaseOperation getOperation();

    @NotNull
    public final LiquibaseExecutionResult execute(@NotNull LiquibaseExecutionContext context) {
        LiquibaseExecutionResult result = context.prepareExecutionResult();
        context.setExecutionThread(Thread.currentThread());
        result.notifyStarted();
        try {
            ensureConfirmed(context.getInput());
            executeOperation(context);
            finishResult(context, TaskStatus.DONE);
        } catch (ElementSkippedException e) {
            finishResult(context, TaskStatus.SKIPPED);
        } catch (RequestCancelledException e) {
            finishResult(context, TaskStatus.CANCELLED);
        } catch (Exception e) {
            result.appendErrorOutput(formatException(e));
            finishResult(context, TaskStatus.FAILED);
        }
        return result;
    }

    protected abstract void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception;

    protected final void checkCanceled(@NotNull LiquibaseExecutionContext context) {
        if (context.isCancellationRequested()) throw new RequestCancelledException("Liquibase execution canceled");
    }

    @NotNull
    protected final String formatException(@NotNull Exception exception) {
        StringWriter output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    protected final void withLiquibaseDatabase(
            @NotNull LiquibaseExecutionContext context,
            boolean readonly,
            @NotNull DBSchema schema,
            @NotNull ThrowableConsumer<Database, Exception> operation) throws SQLException {
        checkCanceled(context);
        ConnectionHandler connection = schema.getConnection();

        withPoolConnection(context, readonly, connection, schema.getSchemaId(), c -> {
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

            operation.accept(database);
            checkCanceled(context);
        });
    }

    protected final void withLiquibaseScope(
            @NotNull LiquibaseExecutionContext context,
            @NotNull ResourceAccessor resourceAccessor,
            @Nullable Consumer<String> sqlConsumer,
            @NotNull ThrowableConsumer<LiquibaseExecutionOutputStream, Exception> operation) throws Exception {
        checkCanceled(context);
        LiquibaseExecutionResult result = context.getResult();
        Map<String, Object> scopeValues = Map.of(
                Scope.Attr.logService.name(), new LiquibaseExecutionLogService(result),
                Scope.Attr.resourceAccessor.name(), resourceAccessor);
        child(scopeValues, () -> {
            try (LiquibaseExecutionOutputStream output = new LiquibaseExecutionOutputStream(result, sqlConsumer)) {
                checkCanceled(context);
                operation.accept(output);
                checkCanceled(context);
            }
        });
        checkCanceled(context);
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

    private void withPoolConnection(
            @NotNull LiquibaseExecutionContext context,
            boolean readonly,
            @NotNull ConnectionHandler connection,
            @Nullable SchemaId schemaId,
            @NotNull ThrowableConsumer<DBNConnection, Exception> operation) throws SQLException {
        checkCanceled(context);
        ConnectionContext connectionContext = new ConnectionContext(
                connection.getProject(),
                connection.getConnectionId(),
                schemaId);
        PooledConnection.call(connectionContext, readonly, c ->
                withClassLoader(LiquibaseExecutionProcessor.class, () -> {
                    try {
                        checkCanceled(context);
                        operation.accept(c);
                        checkCanceled(context);
                        return null;
                    } catch (Throwable e) {
                        if (e instanceof RequestCancelledException requestCancelledException) throw requestCancelledException;
                        throw toSqlException(unwrap(e));
                    }
                }));
    }

    protected static CommandResults executeCommand(String commandName, LiquibaseExecutionOutputStream output, @NonNls Map<String, Object> arguments) throws CommandExecutionException {
        return executeCommand(commandName, output, arguments, Map.of());
    }

    protected static CommandResults executeCommand(
            String commandName,
            LiquibaseExecutionOutputStream output,
            @NonNls Map<String, Object> arguments,
            Map<Class<?>, Object> dependencies) throws CommandExecutionException {
        CommandScope command = new CommandScope(commandName);

        for (String argument : arguments.keySet()) {
            Object value = arguments.get(argument);
            if (value == null || value == LiquibaseCommands.NULL_ARGUMENT) continue;

            command.addArgumentValue(argument, value);
        }

        for (Map.Entry<Class<?>, Object> dependency : dependencies.entrySet()) {
            command.provideDependency(dependency.getKey(), dependency.getValue());
        }

        command.setOutput(output);
        return command.execute();
    }

}
