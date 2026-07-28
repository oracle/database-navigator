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

package com.dbn.liquibase.operation.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public abstract class AbstractLiquibaseOperationResultAction extends ContextAction<LiquibaseOperationResult> {
    protected AbstractLiquibaseOperationResultAction(String text) {
        super(text);
    }

    protected static @NotNull DatabaseLiquibaseManager getLiquibaseManager(@NotNull Project project) {
        return DatabaseLiquibaseManager.getInstance(project);
    }

    protected LiquibaseOperationResult getContext(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.LIQUIBASE_EXECUTION_RESULT);
    }
}
