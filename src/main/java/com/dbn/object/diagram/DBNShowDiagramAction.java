/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.object.diagram;

import com.dbn.object.DBTable;
import com.dbn.object.action.AnObjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DBNShowDiagramAction extends AnObjectAction<DBTable> {
    public DBNShowDiagramAction(DBTable table) {
        super(table);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent event, @NotNull Project project, @NotNull DBTable table) {
        DatabaseDiagramManager.getInstance(project).showDiagram(table, event);
    }

    @Override
    protected void update(@NotNull AnActionEvent event, @NotNull Presentation presentation,
                          @NotNull Project project, @Nullable DBTable table) {
        if (table != null) presentation.setText("Show Database Diagram");
    }
}
