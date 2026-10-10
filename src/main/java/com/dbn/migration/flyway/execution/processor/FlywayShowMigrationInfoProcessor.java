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

package com.dbn.migration.flyway.execution.processor;

import com.dbn.common.task.TaskStatus;
import com.dbn.migration.flyway.execution.FlywayExecutionProcessor;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.operation.FlywayOperationContext;
import com.dbn.migration.flyway.operation.FlywayOperationResult;
import com.dbn.migration.flyway.workspace.FlywayWorkspacePaths;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.InfoResult;
import org.jetbrains.annotations.NotNull;

/**
 * Retrieves the applied and pending migration information from Flyway.
 */
public final class FlywayShowMigrationInfoProcessor extends FlywayExecutionProcessor {
    @Override
    public @NotNull FlywayOperation getOperation() {
        return FlywayOperation.SHOW_MIGRATION_INFO;
    }

    @Override
    protected @NotNull TaskStatus executeOperation(
            @NotNull FlywayOperationContext context,
            @NotNull FlywayWorkspacePaths paths) {
        Flyway flyway = createFlyway(context, paths);
        InfoResult infoResult = flyway.info().getInfoResult();
        FlywayOperationResult result = context.getResult();
        result.setInfoResult(infoResult);
        return TaskStatus.DONE;
    }
}
