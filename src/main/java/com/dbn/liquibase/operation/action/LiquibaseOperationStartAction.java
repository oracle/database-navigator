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

import com.dbn.liquibase.action.LiquibaseSchemaAction;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/** Entry point for one Liquibase operation scoped to a database schema. */
public class LiquibaseOperationStartAction extends LiquibaseSchemaAction {
    private final LiquibaseOperation operation;

    public LiquibaseOperationStartAction(
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperation operation) {
        super(schema);
        this.operation = operation;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        executeOperation(project, operation);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.liquibase.action.Operation_" + operation.name()));
        presentation.setIcon(operation.getActionIcon());
    }
}
