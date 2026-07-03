/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.object.diagram.model;

import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.DatabaseDiagramManager;
import com.intellij.diagram.BaseDiagramProvider;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramElementManager;
import com.intellij.diagram.DiagramNodeContentManager;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import static com.dbn.common.dispose.Failsafe.nd;

public abstract class DBDiagramProvider<T extends DBObject> extends BaseDiagramProvider<T> {
    private final DiagramElementManager<T> elementManager = new DBDiagramElementManager<>(this);
    private final DiagramVfsResolver<T> vfsResolver = new DBDiagramVfsResolver<>();
    private final DBDiagramDescriptor<T> descriptor;

    protected DBDiagramProvider() {
        descriptor = createDescriptor();
    }

    protected abstract DBDiagramDescriptor<T> createDescriptor();

    // DiagramProvider.findByID accepts only [a-zA-Z0-9_-]* identifiers.
    @Override
    public abstract String getID();

    @Override
    public DiagramDataModel<T> createDataModel(
            Project project,
            T element,
            VirtualFile file,
            DiagramPresentationModel presentationModel) {

        DatabaseDiagramManager diagramManager = DatabaseDiagramManager.getInstance(project);
        DBDiagramInput<T> input = diagramManager.createInput(nd(element));
        return new DBDiagramDataModel<>(project, this, input);
    }

    @Override
    public DiagramElementManager<T> getElementManager() {
        return elementManager;
    }

    @Override
    public DiagramVfsResolver<T> getVfsResolver() {
        return vfsResolver;
    }

    @Override
    public DiagramNodeContentManager createNodeContentManager() {
        return new DBDiagramNodeContentManager(descriptor);
    }

    protected final DBDiagramDescriptor<T> getDescriptor() { return descriptor; }
}
