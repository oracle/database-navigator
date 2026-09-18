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

import com.dbn.migration.task.DatabaseMigrationTaskInput;
import org.jetbrains.annotations.NotNull;

/**
 * Common context contract for execution of one database migration operation.
 *
 * <p>The context keeps the operation input typed at the operation boundary while allowing
 * each migration engine to retain its own task-context hierarchy and execution state.</p>
 */
public interface DatabaseMigrationOperationContext<
        O extends DatabaseMigrationOperation,
        I extends DatabaseMigrationTaskInput & DatabaseMigrationOperationInput<O>> {

    @NotNull
    I getInput();

    @NotNull
    default O getOperation() {
        return getInput().getOperation();
    }
}
