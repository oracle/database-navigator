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

package com.dbn.migration.execution;

import com.dbn.migration.operation.DatabaseMigrationOperation;
import com.dbn.migration.task.DatabaseMigrationTaskContext;
import com.dbn.migration.task.DatabaseMigrationTaskInput;
import com.dbn.migration.task.DatabaseMigrationTaskResult;
import org.jetbrains.annotations.NotNull;

/**
 * Executes one migration operation using an engine-owned context and result.
 *
 * <p>The contract exposes only operation identity and the execution entry point. Connection
 * handling, preview behavior, confirmation, logging, cancellation translation, and result
 * details remain responsibilities of the concrete migration engine.</p>
 *
 * @param <O> the concrete operation type handled by the executor
 * @param <I> the concrete input type carried by the execution context
 * @param <C> the concrete execution context type
 * @param <R> the concrete result type returned by the executor
 */
public interface DatabaseMigrationOperationExecutor<
        O extends DatabaseMigrationOperation,
        I extends DatabaseMigrationTaskInput,
        C extends DatabaseMigrationTaskContext<I>,
        R extends DatabaseMigrationTaskResult<I, C, ?>> {
    @NotNull
    O getOperation();

    @NotNull
    R execute(@NotNull C context);
}
