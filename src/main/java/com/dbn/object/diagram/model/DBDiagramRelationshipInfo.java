/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.object.diagram.model;

import com.intellij.diagram.DiagramRelationshipInfo;
import com.intellij.diagram.presentation.DiagramLineType;

import java.awt.Shape;

final class DBDiagramRelationshipInfo implements DiagramRelationshipInfo {
    @Override
    public DiagramLineType getLineType() {
        return DiagramLineType.SOLID;
    }

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public Shape getSourceArrow() {
        return DiagramRelationshipInfo.NONE;
    }

    @Override
    public Shape getTargetArrow() {
        return DiagramRelationshipInfo.STANDARD;
    }
}
