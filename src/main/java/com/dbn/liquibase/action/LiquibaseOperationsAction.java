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

import com.dbn.common.action.ProjectAction;
import com.dbn.liquibase.ui.LiquibaseOperationsDialog;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Dialogs.show;
import static com.dbn.nls.NlsResources.txt;

/** Opens the grouped Liquibase operations overview for a database schema. */
public class LiquibaseOperationsAction extends ProjectAction {
    private final DBSchema schema;

    public LiquibaseOperationsAction(@NotNull DBSchema schema) {
        super(txt("app.liquibase.action.Dashboard"));
        this.schema = schema;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        show(() -> new LiquibaseOperationsDialog(schema));
    }
}
