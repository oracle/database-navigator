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

import com.dbn.common.icon.Icons;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MethodExecutionStartAction extends AbstractMethodExecutionResultAction {

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull MethodExecutionResult executionResult) {

        MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
        executionManager.execute(executionResult.getExecutionInput());
    }

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable MethodExecutionResult target) {


        boolean enabled = target != null &&
                !target.getDebuggerType().isDebug() &&
                target.getExecutionContext().isNot(ExecutionStatus.EXECUTING);

        presentation.setEnabled(enabled);
        presentation.setText("Execute Again");
        presentation.setIcon(Icons.METHOD_EXECUTION_RERUN);
    }
}