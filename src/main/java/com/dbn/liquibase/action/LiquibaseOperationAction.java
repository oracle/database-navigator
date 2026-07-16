/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.action;

import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/** Entry point for one Liquibase operation scoped to a database schema. */
public class LiquibaseOperationAction extends LiquibaseSchemaAction {
    private final LiquibaseOperation operation;

    public LiquibaseOperationAction(
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
