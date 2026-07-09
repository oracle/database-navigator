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
import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramRelationshipInfo;

final class DBDiagramEdge<T extends DBObject> extends DiagramEdgeBase<T> {
    private final String relationshipName;

    DBDiagramEdge(DBDiagramNode<T> source, DBDiagramNode<T> target, DBDiagramRelation relation) {
        super(source, target, relationshipInfo());
        this.relationshipName = relation.name();
    }

    private static DiagramRelationshipInfo relationshipInfo() {
        return new DBDiagramRelationshipInfo();
    }

    @Override
    public String getName() {
        return relationshipName;
    }

}
