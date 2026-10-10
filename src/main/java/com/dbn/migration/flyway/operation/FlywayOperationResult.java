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

import com.dbn.common.task.TaskStatus;
import com.dbn.common.util.ExecutionTiming;
import com.dbn.execution.logging.LogOutput;
import com.dbn.migration.flyway.operation.ui.FlywayOperationResultForm;
import com.dbn.migration.flyway.task.FlywayTaskResult;
import com.dbn.migration.shared.execution.logging.LogOutputBuffer;
import com.dbn.migration.shared.operation.DatabaseMigrationOperationResult;
import org.flywaydb.core.api.output.InfoResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

import static com.dbn.migration.flyway.operation.FlywayFeature.RERUN_ON_SUCCESS;

/**
 * Execution-console result for a Flyway operation.
 */
public class FlywayOperationResult extends FlywayTaskResult<
        FlywayOperationInput, FlywayOperationContext, FlywayOperationResultForm>
        implements DatabaseMigrationOperationResult<
        FlywayOperation, FlywayOperationInput, FlywayOperationContext> {
    private final LogOutputBuffer output;
    private final ExecutionTiming timing = new ExecutionTiming();
    private InfoResult infoResult;
    private ValidateResult validationResult;

    @Nullable
    public InfoResult getInfoResult() {
        return infoResult;
    }

    public void setInfoResult(@Nullable InfoResult infoResult) {
        this.infoResult = infoResult;
        notifyChanged();
    }

    public FlywayOperationResult(@NotNull FlywayOperationContext context) {
        super(context);
        output = new LogOutputBuffer(context.getProject());
    }

    public void setValidationResult(@Nullable ValidateResult validationResult) {
        this.validationResult = validationResult;
        notifyChanged();
    }

    public void notifyStarted() {
        getContext().start();
        timing.start();
        notifyChanged();
    }

    public void notifyFinished(@NotNull TaskStatus status) {
        getContext().finish(status);
        timing.finish();
        notifyChanged();
    }

    public void notifyCancelled() {
        notifyFinished(TaskStatus.CANCELLED);
    }

    public void appendConsoleOutput(@Nullable @Nls String value) {
        if (value == null) return;
        output.appendStdOutput(value);
        notifyChanged();
    }

    public void appendErrorOutput(@Nullable @Nls String value) {
        if (value == null) return;
        output.appendErrOutput(value);
        notifyChanged();
    }

    @NotNull
    public List<LogOutput> getOutput() {
        return output.getOutput();
    }

    @NotNull
    public Duration getExecutionDuration() {
        return timing.getDuration();
    }

    @Override
    public boolean canRerun() {
        TaskStatus status = getStatus();
        if (status == TaskStatus.CANCELLED || status == TaskStatus.FAILED) return true;
        return status == TaskStatus.DONE && getOperation().supports(RERUN_ON_SUCCESS);
    }

    @Nullable
    public ValidateResult getValidationResult() {
        return validationResult;
    }

    @Nullable
    @Override
    public FlywayOperationResultForm createForm() {
        return new FlywayOperationResultForm(this);
    }

    @NotNull
    @Override
    public String getName() {
        return getConnection().getName() + " - " + getInput().getRelevantSchema().getName() + " - " + getOperation().getName();
    }
}
