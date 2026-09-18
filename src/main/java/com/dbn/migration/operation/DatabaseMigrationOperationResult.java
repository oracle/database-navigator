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

package com.dbn.migration.operation;

import com.dbn.migration.task.DatabaseMigrationTaskContext;
import com.dbn.migration.task.DatabaseMigrationTaskInput;
import org.jetbrains.annotations.NotNull;

/**
 * Common result contract for one database migration operation.
 *
 * <p>The result keeps the operation context typed at the operation boundary while allowing
 * each migration engine to provide its own result form, output, items, and rerun behavior.</p>
 */
public interface DatabaseMigrationOperationResult<
        O extends DatabaseMigrationOperation,
        I extends DatabaseMigrationTaskInput & DatabaseMigrationOperationInput<O>,
        C extends DatabaseMigrationTaskContext<I> & DatabaseMigrationOperationContext<O, I>> {

    @NotNull
    C getContext();

    @NotNull
    default O getOperation() {
        return getContext().getOperation();
    }
}
