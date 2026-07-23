/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.liquibase.ui.LiquibaseWorkflowDashboardDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Dialogs.show;
import static com.dbn.nls.NlsResources.txt;

/** Opens the project-level Liquibase workflow dashboard. */
public class LiquibaseWorkflowDashboardOpenAction extends ProjectAction {
    public LiquibaseWorkflowDashboardOpenAction() {
        super(txt("app.menu.action.LiquibaseWorkflowDashboard"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        show(() -> new LiquibaseWorkflowDashboardDialog(project));
    }
}
