/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.execution.common.result.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.execution.ExecutionResult;
import org.jetbrains.annotations.NotNull;

public abstract class ExecutionResultFormBase<T extends ExecutionResult<?>> extends DBNFormBase implements ExecutionResultForm<T>{
    private T executionResult;

    public ExecutionResultFormBase(@NotNull T executionResult) {
        super(null, executionResult.getProject());
        this.executionResult = executionResult;
    }

    @NotNull
    @Override
    public final T getExecutionResult() {
        return Failsafe.nn(executionResult);
    }

    @Override
    public void setExecutionResult(@NotNull T executionResult) {
        if (this.executionResult != executionResult) {
            this.executionResult = Disposer.replace(this.executionResult, executionResult);
            this.executionResult.setPrevious(null);
            rebuildForm();
        }
    }

    protected void rebuildForm(){}

    @Override
    public void disposeInner() {
        Disposer.dispose(executionResult);
        executionResult = null;
        super.disposeInner();
    }
}
