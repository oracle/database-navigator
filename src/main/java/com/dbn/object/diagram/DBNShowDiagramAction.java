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

import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DBNShowDiagramAction extends AnObjectAction<DBObject> {
    public DBNShowDiagramAction(DBObject object) {
        super(object);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent event, @NotNull Project project, @NotNull DBObject object) {
        DatabaseDiagramManager.getInstance(project).showDiagram(object, event);
    }

    @Override
    protected void update(@NotNull AnActionEvent event, @NotNull Presentation presentation,
                          @NotNull Project project, @Nullable DBObject object) {
        if (object != null) presentation.setText("Show Diagram");
    }
}
