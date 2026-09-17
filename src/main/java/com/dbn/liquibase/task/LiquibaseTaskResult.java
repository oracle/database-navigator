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

package com.dbn.liquibase.task;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.execution.ExecutionCancellationAdapter;
import com.dbn.execution.common.result.ui.ExecutionResultForm;
import com.dbn.migration.task.DatabaseMigrationTaskResult;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Base execution result for a Liquibase task.
 *
 * <p>Provides Liquibase icons and cancellation handling for active tasks.</p>
 */
public abstract class LiquibaseTaskResult<
        I extends LiquibaseTaskInput,
        C extends LiquibaseTaskContext<I>,
        F extends ExecutionResultForm>
        extends DatabaseMigrationTaskResult<I, C, F> {

    protected LiquibaseTaskResult(@NotNull C context) {
        super(context);
    }

    @Override
    public ExecutionCancellationAdapter getCancellationAdapter() {
        return getStatus() == TaskStatus.RUNNING
                ? new LiquibaseTaskCancellationAdapter(this)
                : null;
    }

    public abstract boolean canRerun();

    @Override
    public Icon getIcon() {
        return Icons.DB_LIQUIBASE;
    }
}
