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

import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.object.DBTable;
import com.dbn.object.diagram.model.DBNDiagramInput;
import com.dbn.object.diagram.model.DBNDiagramProvider;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.diagram.DiagramProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.uml.core.actions.ShowDiagram;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;

public class DatabaseDiagramManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseDiagramManager";

    private DatabaseDiagramManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    private final Map<DBObjectRef<DBTable>, DBNDiagramInput> preparedInputs = new ConcurrentHashMap<>();

    public static DatabaseDiagramManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseDiagramManager.class);
    }

    @NotNull
    public DBNDiagramInput createModel(@NotNull Collection<? extends DBTable> tables) {
        return new DBNDiagramInput(tables);
    }

    @NotNull
    public DBNDiagramInput createModel(@NotNull DBTable table) {
        DBNDiagramInput input = preparedInputs.remove(table.ref());
        return input == null ? new DBNDiagramInput(table) : input;
    }

    @NotNull
    public DBNDiagramInput prepareModel(@NotNull DBTable table) {
        DBNDiagramInput input = new DBNDiagramInput(table);
        DBObjectRef<DBTable> ref = DBObjectRef.of(table);
        preparedInputs.put(ref, input);
        return input;
    }

    public void showDiagram(@NotNull DBTable table, @NotNull AnActionEvent event) {
        Project project = getProject();
        DataContext dataContext = event.getDataContext();
        DiagramAction diagramAction = new DiagramAction();
        RelativePoint location = diagramAction.getDiagramLocation(dataContext, event);
        Progress.prompt(project, table, true, "Preparing database diagram", "Loading database diagram objects...", progress -> {
            DBNDiagramInput input = prepareModel(table);
            Dispatch.run(dataContext, false, () -> {
                DiagramProvider<?> provider = DiagramProvider.findByID(DBNDiagramProvider.ID);
                if (provider == null) return;
                diagramAction.showDiagram(project, provider, table, input.getDatabaseTables(), location);
            });
        });
    }

    private static class DiagramAction extends ShowDiagram {
        DiagramAction() {
            super(true, false);
        }

        RelativePoint getDiagramLocation(DataContext dataContext, AnActionEvent event) {
            return getLocation(dataContext, event);
        }

        @SuppressWarnings("unchecked")
        void showDiagram(Project project, DiagramProvider<?> provider, DBTable table,
                         Collection<DBTable> tables, RelativePoint location) {
            List<DBTable> seedTables = new ArrayList<>(tables.size() + 1);
            seedTables.add(table);
            seedTables.addAll(tables);
            var seed = createSeed(project, (DiagramProvider<Object>) provider, table,
                    new ArrayList<>(seedTables));
            CompletableFuture<Void> shown = show(seed, location, null).toCompletableFuture();
            shown.exceptionally(exception -> {
                com.dbn.diagnostics.Diagnostics.conditionallyLog(exception);
                return null;
            });
        }
    }

}
