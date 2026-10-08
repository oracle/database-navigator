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

package com.dbn.migration.liquibase.execution.processor;

import com.dbn.migration.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.migration.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.visitor.AbstractChangeExecListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared changeset-item lifecycle handling for Liquibase execution listeners. */
abstract class LiquibaseChangeSetListener extends AbstractChangeExecListener {
    protected final LiquibaseOperationResult result;

    LiquibaseChangeSetListener(@NotNull LiquibaseOperationResult result) {
        this.result = result;
    }

    protected final void startProcessing(@NotNull ChangeSet changeSet) {
        LiquibaseChangeSetItem item = result.ensureChangeSetItem(changeSet);
        item.startProcessing();
        result.notifyItemsChanged();
    }

    protected final void finishProcessing(
            @NotNull ChangeSet changeSet,
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        LiquibaseChangeSetItem item = result.ensureChangeSetItem(changeSet);
        item.finishProcessing();
        item.updateStatus(status, message);
        result.notifyItemsChanged();
    }

    protected final void failProcessing(@NotNull ChangeSet changeSet, @NotNull Exception exception) {
        finishProcessing(changeSet, LiquibaseExecutionItemStatus.FAILED, exception.getMessage());
    }
}
