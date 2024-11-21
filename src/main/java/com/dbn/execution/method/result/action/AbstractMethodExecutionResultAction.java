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

package com.dbn.execution.method.result.action;

import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.ExecutionResult;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractMethodExecutionResultAction extends ContextAction<MethodExecutionResult> {

    protected MethodExecutionResult getTarget(@NotNull AnActionEvent e) {
        MethodExecutionResult result = e.getData(DataKeys.METHOD_EXECUTION_RESULT);
        if (result != null) return result;

        Project project = e.getProject();
        if (project == null) return result;

        ExecutionManager executionManager = ExecutionManager.getInstance(project);
        ExecutionResult executionResult = executionManager.getSelectedExecutionResult();
        if (executionResult instanceof MethodExecutionResult) {
            return (MethodExecutionResult) executionResult;
        }

        return null;
    }
}
