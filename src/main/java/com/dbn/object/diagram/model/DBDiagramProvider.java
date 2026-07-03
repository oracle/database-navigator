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

import com.dbn.common.thread.Dispatch;
import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.DatabaseDiagramManager;
import com.dbn.object.type.DBObjectType;
import com.intellij.diagram.BaseDiagramProvider;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramElementManager;
import com.intellij.diagram.DiagramNodeContentManager;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.diagram.DiagramProvider;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.diagram.presentation.DiagramState;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;

import java.util.Collection;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
public abstract class DBDiagramProvider<T extends DBObject> extends BaseDiagramProvider<T> {
    private final DiagramElementManager<T> elementManager = new DBDiagramElementManager<>(this);
    private final DiagramVfsResolver<T> vfsResolver = new DBDiagramVfsResolver<>();
    private final DBDiagramType diagramType;

    protected DBDiagramProvider(DBDiagramType diagramType) {
        this.diagramType = diagramType;
    }

    public DBDiagramInput<T> createInput(T source) {
        return new DBDiagramInput<>(this, source);
    }

    public abstract Collection<T> getRootObjects(T source);

    public abstract Collection<? extends DBObject> getChildObjects(T root);

    public abstract Collection<DBDiagramRelation<T>> getRelations(Collection<? extends T> roots);

    public Object[] getNodeItems(T root) {
        return getChildObjects(root).toArray();
    }

    public String getElementTitle(T root) {
        return root.getName();
    }

    public String getNodeTooltip(T root) {
        return root.getQualifiedNameWithType();
    }

    public String getItemName(Object item, DiagramBuilder builder) {
        return item instanceof DBObject object ? object.getName() : "";
    }

    public String getItemType(Object item) {
        return "";
    }

    public DiagramCategory[] getContentCategories() {
        return DiagramCategory.EMPTY_ARRAY;
    }

    public boolean isInCategory(Object node, Object child, DiagramCategory category, DiagramBuilder builder) {
        return false;
    }

    public boolean isInCategory(Object child, DiagramCategory category, DiagramState state) {
        return false;
    }

    @Override
    public final String getID() {
        return diagramType.getProviderId();
    }

    @Override
    public final String getPresentableName() {
        return diagramType.getPresentableName();
    }

    @SuppressWarnings("unchecked")
    public static <T extends DBObject> DBDiagramProvider<T> get(DBDiagramType diagramType) {
        DiagramProvider<?> diagramProvider = DiagramProvider.findByID(diagramType.getProviderId());
        if (!(diagramProvider instanceof DBDiagramProvider<?> provider)) {
            throw new IllegalArgumentException("Unknown DBN diagram provider: " + diagramType.getProviderId());
        }
        return (DBDiagramProvider<T>) provider;
    }

    protected static DiagramCategory createCategory(DBObjectType objectType) {
        return new DiagramCategory(() -> objectType.getListDisplayName(), objectType.getListIcon());
    }

    @Override
    public DiagramCategory[] getAllContentCategories() {
        return getContentCategories();
    }

    @Override
    public DiagramDataModel<T> createDataModel(
            Project project,
            T element,
            VirtualFile file,
            DiagramPresentationModel presentationModel) {

        if (element == null) {
            // close ghost diagrams from previous sessions
            // TODO is there a better way to prevent reopening diagrams? we have no control over the UmlFileSystem
            FileEditorManager instance = FileEditorManager.getInstance(project);
            Dispatch.run(ModalityState.nonModal(), () -> instance.closeFile(file));
        }
        DatabaseDiagramManager diagramManager = DatabaseDiagramManager.getInstance(project);
        DBDiagramInput<T> input = diagramManager.createDiagramInput(nd(element));
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
        return new DBDiagramNodeContentManager(this);
    }
}
