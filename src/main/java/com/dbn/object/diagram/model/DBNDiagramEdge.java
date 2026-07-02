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
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramRelationshipInfo;

final class DBNDiagramEdge extends DiagramEdgeBase<DBTable> {
    private final DBObjectRef<DBColumn> column;
    private final String relationshipName;

    DBNDiagramEdge(DBNDiagramNode source, DBNDiagramNode target, DBColumn column) {
        super(source, target, relationshipInfo(source, target, column));
        this.column = DBObjectRef.of(column);
        DBColumn foreignKeyColumn = column.getForeignKeyColumn();
        String targetColumnName = foreignKeyColumn == null
                ? column.getName()
                : foreignKeyColumn.getName();

        this.relationshipName = source.getObjectName() + "." +
                column.getName() + " -> " +
                target.getObjectName() + "." + targetColumnName;
    }

    private static DiagramRelationshipInfo relationshipInfo(DBNDiagramNode source,
                                                             DBNDiagramNode target,
                                                             DBColumn column) {
        return relationshipInfo();
    }

    private static DiagramRelationshipInfo relationshipInfo() {
        return new DBNDiagramRelationshipInfo();
    }

    @Override
    public String getName() {
        return relationshipName;
    }

}
