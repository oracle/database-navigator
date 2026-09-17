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

package com.dbn.migration.workflow;

import com.dbn.migration.operation.DatabaseMigrationOperation;
import com.dbn.migration.task.DatabaseMigrationTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Describes a reusable sequence of database migration operations.
 *
 * <p>The workflow contract contains only the ordered operation plan and the task metadata
 * needed to present it. Input propagation, capability aggregation, execution state,
 * cancellation, and rerun behavior remain responsibilities of the concrete engine.</p>
 */
public interface DatabaseMigrationWorkflow extends DatabaseMigrationTask {
    /**
     * Returns the engine-defined category used to group this workflow.
     */
    @NotNull
    DatabaseMigrationWorkflowCategory getCategory();

    /**
     * Returns the workflow title used by detailed workflow views.
     */
    @NotNull
    String getTitle();

    /**
     * Returns the workflow description used by detailed workflow views.
     */
    @NotNull
    String getDescription();

    /**
     * Returns the short workflow hint used by selection and dashboard views.
     */
    @NotNull
    String getHint();

    /**
     * Returns the operations executed by this workflow in their defined order.
     */
    @NotNull
    List<? extends DatabaseMigrationOperation> getOperations();

    default boolean includesOperation(@NotNull DatabaseMigrationOperation operation) {
        return getOperations().contains(operation);
    }
}
