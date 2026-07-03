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
import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.model.DBDiagramInput;
import com.dbn.object.diagram.model.DBDiagramProvider;
import com.dbn.object.diagram.model.DBDiagramType;
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
import static com.dbn.common.util.Unsafe.cast;

public class DatabaseDiagramManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseDiagramManager";

    private DatabaseDiagramManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    private final Map<DBObjectRef<?>, DBDiagramInput<?>> preparedInputs = new ConcurrentHashMap<>();

    public static DatabaseDiagramManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseDiagramManager.class);
    }

    @NotNull
    public <T extends DBObject> DBDiagramInput<T> createDiagramInput(@NotNull T source) {
        DBDiagramType diagramType = resolveDiagramType(source);
        DBDiagramProvider<T> provider = DBDiagramProvider.get(diagramType);
        DBDiagramInput<T> input = cast(preparedInputs.remove(source.ref()));
        return input == null ? provider.createInput(source) : input;
    }

    @NotNull
    public <T extends DBObject> DBDiagramInput<T> prepareDiagramInput(@NotNull T source) {
        DBDiagramType diagramType = resolveDiagramType(source);
        DBDiagramProvider<T> provider = DBDiagramProvider.get(diagramType);
        DBDiagramInput<T> input = provider.createInput(source);
        preparedInputs.put(source.ref(), input);
        return input;
    }

    public <T extends DBObject> void showDiagram(@NotNull T source, @NotNull AnActionEvent event) {
        DBDiagramType diagramType = resolveDiagramType(source);
        Project project = getProject();
        DataContext dataContext = event.getDataContext();
        DiagramAction<T> diagramAction = new DiagramAction<>();
        RelativePoint location = diagramAction.getDiagramLocation(dataContext, event);
        Progress.prompt(project, source, true, "Preparing database diagram", "Loading database diagram objects...", progress -> {
            DBDiagramInput<T> input = prepareDiagramInput(source);
            Dispatch.run(dataContext, false, () -> {
                DiagramProvider<?> provider = DiagramProvider.findByID(diagramType.getProviderId());
                if (provider == null) return;
                diagramAction.showDiagram(project, provider, source, input.getRoots(), location);
            });
        });
    }

    @NotNull
    private DBDiagramType resolveDiagramType(DBObject source) {
        DBDiagramType diagramType = DBDiagramType.forObjectType(source.getObjectType());
        if (diagramType == null) {
            throw new IllegalArgumentException("Unsupported diagram object type: " + source.getObjectType());
        }
        return diagramType;
    }

    private static class DiagramAction<R extends DBObject> extends ShowDiagram {
        DiagramAction() {
            super(true, false);
        }

        RelativePoint getDiagramLocation(DataContext dataContext, AnActionEvent event) {
            return getLocation(dataContext, event);
        }

        @SuppressWarnings("unchecked")
        void showDiagram(Project project, DiagramProvider<?> provider, R source,
                         Collection<R> roots, RelativePoint location) {
            List<R> seedElements = new ArrayList<>(roots.size() + 1);
            seedElements.add(source);
            seedElements.addAll(roots);
            var seed = createSeed(project, (DiagramProvider<Object>) provider, source, new ArrayList<>(seedElements));

            CompletableFuture<Void> shown = show(seed, location, null).toCompletableFuture();
            shown.exceptionally(exception -> {
                com.dbn.diagnostics.Diagnostics.conditionallyLog(exception);
                return null;
            });
        }
    }

}
