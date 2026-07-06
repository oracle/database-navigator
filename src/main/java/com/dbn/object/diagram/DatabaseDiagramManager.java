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
import static com.dbn.nls.NlsResources.txt;

public class DatabaseDiagramManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseDiagramManager";

    private DatabaseDiagramManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    private final Map<DBObjectRef<?>, DBDiagramInput<?>> preparedInputs = new ConcurrentHashMap<>();

    public static DatabaseDiagramManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseDiagramManager.class);
    }

    /**
     * Creates the input consumed by IntelliJ when it initializes the diagram model.
     *
     * <p>If {@link #prepareDiagramInput(DBObject)} already built the input during the
     * background preparation phase, this method consumes that staged input. This
     * handoff avoids repeating database traversal on the EDT. If no staged input is
     * available, the provider creates one synchronously as a fallback.</p>
     */
    public <T extends DBObject> DBDiagramInput<T> createDiagramInput(@NotNull T source) {
        DBDiagramProvider<T> provider = getDiagramProvider(source);
        DBDiagramInput<T> input = cast(preparedInputs.remove(source.ref()));
        return input == null ? provider.createInput(source) : input;
    }

    /**
     * Builds and stages diagram input for later consumption by
     * {@link #createDiagramInput(DBObject)}.
     *
     * <p>This method is intended to run during the background/progress phase. The
     * staged input is kept by source reference so IntelliJ's later model-creation
     * callback can reuse it instead of loading database objects again on the EDT.</p>
     */
    @NotNull
    public <T extends DBObject> DBDiagramInput<T> prepareDiagramInput(@NotNull T source) {
        DBDiagramProvider<T> provider = getDiagramProvider(source);
        DBDiagramInput<T> input = provider.createInput(source);
        preparedInputs.put(source.ref(), input);
        return input;
    }

    public <T extends DBObject> void showDiagram(@NotNull T source, @NotNull AnActionEvent event) {
        DBDiagramProvider<T> provider = getDiagramProvider(source);
        DBDiagramType diagramType = provider.getDiagramType();

        DataContext dataContext = event.getDataContext();
        DiagramAction<T> diagramAction = new DiagramAction<>();
        RelativePoint location = diagramAction.getDiagramLocation(dataContext, event);

        Project project = getProject();
        Progress.prompt(project, source, true,
                txt("prc.diagram.title.Preparing_" + diagramType),
                txt("prc.diagram.text.LoadingObjects_" + diagramType, source.getQualifiedName()), progress -> {
            DBDiagramInput<T> input = prepareDiagramInput(source);
            Dispatch.run(dataContext, false, () -> {
                diagramAction.showDiagram(project, provider, source, input.getRoots(), location);
            });
        });
    }

    private <T extends DBObject> DBDiagramProvider<T> getDiagramProvider(@NotNull T source) {
        DBDiagramType diagramType = DBDiagramType.forObjectType(source.getObjectType());
        if (diagramType == null) {
            throw new IllegalArgumentException("Unsupported diagram object type: " + source.getObjectType());
        }
        return DBDiagramProvider.get(diagramType);
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
