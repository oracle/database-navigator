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
import org.flywaydb.core.api.output.ValidateResult;
import org.jetbrains.annotations.NotNull;

/**
 * Executes Flyway migration validation.
 */
public final class FlywayValidateMigrationsProcessor extends FlywayExecutionProcessor {
    @Override
    public @NotNull FlywayOperation getOperation() {
        return FlywayOperation.VALIDATE_MIGRATIONS;
    }

    @Override
    protected @NotNull TaskStatus executeOperation(
            @NotNull FlywayOperationContext context,
            @NotNull FlywayWorkspacePaths paths) {
        Flyway flyway = createFlyway(context, paths);
        ValidateResult validationResult = flyway.validateWithResult();
        FlywayOperationResult result = context.getResult();
        result.setValidationResult(validationResult);
        result.appendConsoleOutput("Flyway validation completed for " + context.getSchema().getName());
        return validationResult.validationSuccessful ? TaskStatus.DONE : TaskStatus.FAILED;
    }
}
