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

package com.dbn.liquibase.task;

import com.dbn.execution.ExecutionCancellationAdapter;
import org.jetbrains.annotations.Nls;

import static com.dbn.liquibase.DatabaseLiquibaseManager.getInstance;
import static com.dbn.nls.NlsResources.txt;

final class LiquibaseTaskCancellationAdapter implements ExecutionCancellationAdapter {
    private final LiquibaseTaskResult<?, ?, ?> result;

    LiquibaseTaskCancellationAdapter(LiquibaseTaskResult<?, ?, ?> result) {
        this.result = result;
    }

    @Override
    public @Nls String getConfirmationTitle() {
        return txt("msg.liquibase.title.ExecutionActive");
    }

    @Override
    public @Nls String getConfirmationMessage() {
        return txt("msg.liquibase.question.ExecutionActive");
    }

    @Override
    public void cancelExecution() {
        getInstance(result.getProject()).cancelTask(result);
    }
}
