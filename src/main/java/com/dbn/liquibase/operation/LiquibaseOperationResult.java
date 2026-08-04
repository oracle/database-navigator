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

package com.dbn.liquibase.operation;

import com.dbn.common.task.TaskStatus;
import com.dbn.common.util.ExecutionTiming;
import com.dbn.execution.logging.LogOutput;
import com.dbn.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.liquibase.execution.LiquibaseComparisonItem;
import com.dbn.liquibase.execution.LiquibaseComparisonItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionItem;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseLockItem;
import com.dbn.liquibase.execution.LiquibaseSnapshotItem;
import com.dbn.liquibase.execution.logging.LogOutputBuffer;
import com.dbn.liquibase.operation.ui.LiquibaseOperationResultForm;
import com.dbn.liquibase.task.LiquibaseTaskResult;
import com.dbn.liquibase.workflow.LiquibaseWorkflowContext;
import com.dbn.liquibase.workflow.LiquibaseWorkflowResult;
import liquibase.changelog.ChangeSet;
import liquibase.diff.ObjectDifferences;
import liquibase.structure.DatabaseObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.dbn.liquibase.operation.LiquibaseFeature.RERUN_ON_SUCCESS;

/** Execution-console result for a Liquibase operation and its console output. */
@Getter
@Setter
public class LiquibaseOperationResult extends LiquibaseTaskResult<
        LiquibaseOperationInput, LiquibaseOperationContext, LiquibaseOperationResultForm> {

    private Path changelogPath;
    private Path documentationPath;
    private String databaseChangeLogTableName = "DATABASECHANGELOG";
    private String databaseChangeLogLockTableName = "DATABASECHANGELOGLOCK";
    private final LogOutputBuffer output;
    private final StringBuilder sqlOutput = new StringBuilder();
    private final ExecutionTiming timing = new ExecutionTiming();
    private final Map<String, LiquibaseSnapshotItem> snapshotItems = new LinkedHashMap<>();
    private final Map<String, LiquibaseChangeSetItem> changeSetItems = new LinkedHashMap<>();
    private final Map<String, LiquibaseComparisonItem> comparisonItems = new LinkedHashMap<>();
    private final Map<String, LiquibaseLockItem> lockItems = new LinkedHashMap<>();

    public LiquibaseOperationResult(@NotNull LiquibaseOperationContext context) {
        super(context);
        this.output = new LogOutputBuffer(context.getProject());
    }

    @Override
    public void disposeInner() {
        snapshotItems.clear();
        changeSetItems.clear();
        comparisonItems.clear();
        lockItems.clear();
        super.disposeInner();
    }

    @Override
    @Delegate
    public @NotNull LiquibaseOperationInput getInput() {
        return super.getInput();
    }

    @NotNull
    public List<LiquibaseSnapshotItem> getSnapshotItems() {
        synchronized (snapshotItems) {
            return new ArrayList<>(snapshotItems.values());
        }
    }

    @NotNull
    public List<LiquibaseChangeSetItem> getChangeSetItems() {
        synchronized (changeSetItems) {
            return new ArrayList<>(changeSetItems.values());
        }
    }

    @NotNull
    public List<LiquibaseComparisonItem> getComparisonItems() {
        synchronized (comparisonItems) {
            return new ArrayList<>(comparisonItems.values());
        }
    }

    @NotNull
    public List<LiquibaseLockItem> getLockItems() {
        synchronized (lockItems) {
            return new ArrayList<>(lockItems.values());
        }
    }

    @NotNull
    public LiquibaseSnapshotItem ensureSnapshotItem(@NotNull DatabaseObject databaseObject) {
        return ensureItem(snapshotItems,
                buildDatabaseObjectKey(databaseObject),
                () -> new LiquibaseSnapshotItem(databaseObject));
    }

    @NotNull
    public LiquibaseChangeSetItem ensureChangeSetItem(@NotNull ChangeSet changeSet) {
        return ensureItem(changeSetItems,
                buildChangeSetKey(changeSet),
                () -> new LiquibaseChangeSetItem(changeSet));
    }

    @NotNull
    public LiquibaseChangeSetItem ensureChangeSetItem(
            @NotNull ChangeSet changeSet,
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        return ensureItem(changeSetItems,
                buildChangeSetKey(changeSet),
                () -> new LiquibaseChangeSetItem(changeSet, status, message));
    }

    @NotNull
    public LiquibaseComparisonItem ensureComparisonItem(
            @Nullable DatabaseObject sourceObject,
            @Nullable DatabaseObject targetObject,
            @NotNull LiquibaseComparisonItemStatus status,
            @Nullable ObjectDifferences differences) {
        LiquibaseComparisonItem item = new LiquibaseComparisonItem(sourceObject, targetObject, status, differences);
        synchronized (comparisonItems) {
            LiquibaseComparisonItem existing = comparisonItems.get(item.getKey());
            if (existing != null) return existing;
            comparisonItems.put(item.getKey(), item);
        }
        notifyItemsChanged();
        return item;
    }

    @NotNull
    public LiquibaseLockItem ensureLockItem(@NotNull liquibase.lockservice.DatabaseChangeLogLock lock) {
        return ensureItem(lockItems, Integer.toString(lock.getId()), () -> new LiquibaseLockItem(lock));
    }

    @NotNull
    private <I extends LiquibaseExecutionItem> I ensureItem(
            @NotNull Map<String, I> items,
            @NotNull String key,
            @NotNull Supplier<I> factory) {
        I item;
        boolean created = false;
        synchronized (items) {
            item = items.get(key);
            if (item == null) {
                item = factory.get();
                items.put(key, item);
                created = true;
            }
        }
        if (created) notifyItemsChanged();
        return item;
    }

    @NotNull
    public static String buildDatabaseObjectKey(@NotNull DatabaseObject databaseObject) {
        String schemaName = databaseObject.getSchema() == null ? "" : databaseObject.getSchema().getName();
        return databaseObject.getObjectTypeName() + ':' + schemaName + ':' + databaseObject.getName();
    }

    @NotNull
    public static String buildChangeSetKey(@NotNull ChangeSet changeSet) {
        return changeSet.getFilePath() + ':' + changeSet.getAuthor() + ':' + changeSet.getId();
    }

    public void updateExecutionItem(
            @NotNull LiquibaseSnapshotItem item,
            @NotNull DatabaseObject databaseObject,
            @NotNull LiquibaseExecutionItemStatus status,
            String message) {
        item.update(databaseObject, status, message);
        notifyItemsChanged();
    }

    public void notifyItemsChanged() {
        notifyChanged();
    }

    public void setLiquibaseTableNames(
            @NotNull String databaseChangeLogTableName,
            @NotNull String databaseChangeLogLockTableName) {
        boolean changed = !databaseChangeLogTableName.equals(this.databaseChangeLogTableName) ||
                !databaseChangeLogLockTableName.equals(this.databaseChangeLogLockTableName);
        this.databaseChangeLogTableName = databaseChangeLogTableName;
        this.databaseChangeLogLockTableName = databaseChangeLogLockTableName;
        if (changed) notifyItemsChanged();
    }

    public void notifyStarted() {
        getContext().start();
        timing.start();
        notifyItemsChanged();
    }

    public void notifyFinished(@NotNull TaskStatus status) {
        getContext().finish(status);
        timing.finish();
        notifyItemsChanged();
    }

    public void notifyCancelled() {
        getContext().finish(TaskStatus.CANCELLED);
        timing.finish();
        notifyItemsChanged();
    }

    public void notifyPaused() {
        getContext().pause();
        notifyItemsChanged();
    }

    public boolean canRerun() {
        TaskStatus status = getStatus();
        if (status == TaskStatus.CANCELLED) return true;
        if (status == TaskStatus.FAILED) return true;
        return status == TaskStatus.DONE && getOperation().supports(RERUN_ON_SUCCESS);
    }

    @NotNull
    public Duration getExecutionDuration() {
        return timing.getDuration();
    }

    public void appendConsoleOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendStdOutput(output);
        notifyItemsChanged();
    }

    public void appendSqlOutput(@Nullable String output) {
        if (output == null || output.isEmpty()) return;
        synchronized (sqlOutput) {
            sqlOutput.append(output);
            if (!output.endsWith("\n")) sqlOutput.append('\n');
        }
        notifyItemsChanged();
    }

    @NotNull
    public String getSqlOutput() {
        synchronized (sqlOutput) {
            return sqlOutput.toString();
        }
    }

    public void appendErrorOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendErrOutput(output);
        notifyItemsChanged();
    }

    public void appendInfoOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendSysOutput(getConnection(), output);
        notifyItemsChanged();
    }

    @NotNull
    public List<LogOutput> getOutput() {
        return output.getOutput();
    }

    @Nullable
    @Override
    public LiquibaseOperationResultForm createForm() {
        return new LiquibaseOperationResultForm(this, false);
    }

    @NotNull
    @Override
    public String getName() {
        return getConnection().getName() + " - " + getRelevantSchema().getName() + " - " + getOperation().getName();
    }

    public LiquibaseWorkflowResult getWorkflowResult() {
        LiquibaseWorkflowContext workflowContext = getContext().getWorkflowContext();
        return workflowContext == null ? null : workflowContext.getResult();
    }
}
