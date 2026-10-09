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

package com.dbn.migration.flyway.operation;

import com.dbn.migration.flyway.task.FlywayTaskContext;
import com.dbn.migration.shared.operation.DatabaseMigrationOperationContext;
import com.dbn.object.DBSchema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Per-run state shared by a Flyway processor and its execution result.
 */
@Getter
@Setter
public class FlywayOperationContext
        extends FlywayTaskContext<FlywayOperationInput>
        implements DatabaseMigrationOperationContext<FlywayOperation, FlywayOperationInput> {
    private final FlywayOperationResult result = new FlywayOperationResult(this);
    private volatile Thread executionThread;

    public FlywayOperationContext(@NotNull FlywayOperationInput input) {
        super(input);
    }

    public void cancel() {
        super.cancel();
        Thread thread = executionThread;
        if (thread != null) thread.interrupt();
    }

    @NotNull
    public DBSchema getSchema() {
        return getInput().getRelevantSchema();
    }

    public void clearExecutionThread() {
        executionThread = null;
    }
}
