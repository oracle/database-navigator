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
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

final class DBDiagramDataModel<T extends DBObject> extends DiagramDataModel<T> {
    private final List<DBDiagramNode<T>> nodes;
    private final List<DiagramEdge<T>> edges;
    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    DBDiagramDataModel(@NotNull Project project, @NotNull DBDiagramProvider<T> provider, @NotNull DBDiagramInput<T> input) {
        // The two-argument constructor can leave the content manager lazy/null
        // while the UML editor is creating its toolbar.  Supply the provider's
        // manager explicitly so category configuration (Columns) is available
        // when the editor is initialized.
        super(project, provider, provider.createNodeContentManager());
        Map<T, DBDiagramNode<T>> nodeMap = new LinkedHashMap<>();
        for (T root : input.getRoots()) {
            nodeMap.put(root, new DBDiagramNode<>(root, provider));
        }
        this.nodes = new ArrayList<>(nodeMap.values());
        this.edges = new ArrayList<>();
        if (!input.getRoots().isEmpty()) {
            DBDiagramDescriptor<T> descriptor = getDiagramProvider().getDescriptor();
            for (DBDiagramRelation<T> relation : descriptor.getRelations(input.getRoots())) {
                DBDiagramNode<T> sourceNode = nodeMap.get(relation.source());
                DBDiagramNode<T> targetNode = nodeMap.get(relation.target());
                if (sourceNode != null && targetNode != null) {
                    edges.add(new DBDiagramEdge<>(sourceNode, targetNode, relation));
                }
            }
        }
        setOriginalElement(input.getRoots().isEmpty() ? null : input.getRoots().get(0));
    }

    private @NotNull DBDiagramProvider<T> getDiagramProvider() {
        return (DBDiagramProvider<T>) getProvider();
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return modificationTracker;
    }

    @Override
    public Collection<? extends DiagramNode<T>> getNodes() {
        return unmodifiableList(nodes);
    }

    @Override
    public String getNodeName(DiagramNode<T> node) {
        return ((DBDiagramNode) node).getObjectName();
    }

    @Override
    public DiagramNode<T> addElement(T element) {
        for (DBDiagramNode<T> node : nodes) {
            DBObject nodeElement = node.getIdentifyingElement();
            if (nodeElement.ref().equals(element.ref())) return node;
        }
        DBDiagramNode<T> node = new DBDiagramNode<>(element, getProvider());
        nodes.add(node);
        return node;
    }

    @Override
    public Collection<? extends DiagramEdge<T>> getEdges() {
        return unmodifiableList(edges);
    }

    @Override
    public void dispose() {
    }
}
