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

package com.dbn.liquibase.workflow;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.task.TaskStatus;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.liquibase.task.LiquibaseTaskResult;
import com.dbn.liquibase.workflow.ui.LiquibaseWorkflowResultForm;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.liquibase.operation.LiquibaseFeature.RERUN_ON_SUCCESS;

/** Execution-console result aggregating the operation results produced by a Liquibase workflow. */
@Getter
public class LiquibaseWorkflowResult extends LiquibaseTaskResult<
        LiquibaseWorkflowInput,
        LiquibaseWorkflowContext,
        LiquibaseWorkflowResultForm> {

    private final List<LiquibaseOperationResult> operationResults = new ArrayList<>();

    public LiquibaseWorkflowResult(@NotNull LiquibaseWorkflowContext context) {
        super(context);
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(operationResults);
        super.disposeInner();
    }

    @Override
    @Delegate
    public LiquibaseWorkflowInput getInput() {
        return super.getInput();
    }

    @NotNull
    public List<LiquibaseOperationResult> getOperationResults() {
        synchronized (operationResults) {
            return new ArrayList<>(operationResults);
        }
    }

    public void addResult(@NotNull LiquibaseOperationResult result) {
        synchronized (operationResults) {
            operationResults.add(result);
        }
        result.addListener(this::notifyChanged);
        notifyChanged();
    }

    public boolean canRerun() {
        TaskStatus status = getStatus();
        if (status == TaskStatus.CANCELLED) return true;
        if (status == TaskStatus.FAILED) return true;
        if (status == TaskStatus.SKIPPED) return true;
        return status == TaskStatus.DONE && getInput().getSupport().supports(RERUN_ON_SUCCESS);
    }

    @Nullable
    @Override
    public LiquibaseWorkflowResultForm createForm() {
        return new LiquibaseWorkflowResultForm(this);
    }

    @NotNull
    @Override
    public String getName() {
        return getConnection().getName() + " - " +
                getRelevantSchema().getName() + " - " +
                getWorkflow().getTitle();
    }

    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.LIQUIBASE_WORKFLOW_RESULT.is(dataId)) return this;
        return super.getData(dataId);
    }
}
