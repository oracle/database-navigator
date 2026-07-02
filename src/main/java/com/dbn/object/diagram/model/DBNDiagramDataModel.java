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

import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DBNDiagramDataModel extends DiagramDataModel<DBTable> {
    private final List<DBNDiagramNode> nodes;
    private final List<DiagramEdge<DBTable>> edges;
    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    DBNDiagramDataModel(@NotNull Project project, @NotNull DiagramProvider<DBTable> provider, @NotNull DBNDiagramInput input) {
        // The two-argument constructor can leave the content manager lazy/null
        // while the UML editor is creating its toolbar.  Supply the provider's
        // manager explicitly so category configuration (Columns) is available
        // when the editor is initialized.
        super(project, provider, provider.createNodeContentManager());
        Map<DBTable, DBNDiagramNode> nodeMap = new LinkedHashMap<>();
        for (DBTable table : input.getTables()) {
            nodeMap.put(table, new DBNDiagramNode(table, provider));
        }
        this.nodes = new ArrayList<>(nodeMap.values());
        this.edges = new ArrayList<>();
        for (DBTable source : input.getTables()) {
            for (DBColumn column : source.getColumns()) {
                DBColumn targetColumn = column.getForeignKeyColumn();
                if (targetColumn == null || !(targetColumn.getDataset() instanceof DBTable target)) continue;
                DBNDiagramNode sourceNode = nodeMap.get(source);
                DBNDiagramNode targetNode = nodeMap.get(target);
                if (sourceNode != null && targetNode != null) {
                    edges.add(new DBNDiagramEdge(sourceNode, targetNode, column));
                }
            }
        }
        setOriginalElement(input.getTables().isEmpty() ? null : input.getTables().get(0));
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return modificationTracker;
    }

    @Override
    public Collection<? extends DiagramNode<DBTable>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public String getNodeName(DiagramNode<DBTable> node) {
        return ((DBNDiagramNode) node).getObjectName();
    }

    @Override
    public DiagramNode<DBTable> addElement(DBTable element) {
        for (DBNDiagramNode node : nodes) {
            DBTable nodeElement = node.getIdentifyingElement();
            if (nodeElement.ref().equals(element.ref())) return node;
        }
        DBNDiagramNode node = new DBNDiagramNode(element, getProvider());
        nodes.add(node);
        return node;
    }

    @Override
    public Collection<? extends DiagramEdge<DBTable>> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public void dispose() {
    }
}
