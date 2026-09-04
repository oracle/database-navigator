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

import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.task.LiquibaseTaskContext;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Per-run state shared by a workflow and its operation results. */
@Getter
public class LiquibaseWorkflowContext extends LiquibaseTaskContext<LiquibaseWorkflowInput> {
    private final LiquibaseWorkflowResult result = new  LiquibaseWorkflowResult(this);
    private final LiquibaseWorkflowExecutor executor = new LiquibaseWorkflowExecutor(result);
    private final List<LiquibaseOperation> operations;
    private int operationIndex = -1;

    public LiquibaseWorkflowContext(@NotNull LiquibaseWorkflowInput input) {
        super(input);
        this.operations = input.getWorkflow().getOperations();
    }

    @Nullable
    public LiquibaseOperation getCurrentOperation() {
        return operationIndex >= 0 && operationIndex < operations.size() ? operations.get(operationIndex) : null;
    }

    public void startOperation(int index) {
        operationIndex = index;
        start();
    }

}
