/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.object.diagram.model;

import com.intellij.diagram.AbstractDiagramNodeContentManager;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;

final class DBDiagramNodeContentManager extends AbstractDiagramNodeContentManager {
    private final DBDiagramDescriptor<?> descriptor;

    DBDiagramNodeContentManager(DBDiagramDescriptor<?> descriptor) {
        this.descriptor = descriptor;
        for (DiagramCategory category : descriptor.getContentCategories()) setCategoryEnabled(category, true);
    }

    @Override
    public DiagramCategory[] getContentCategories() {
        return descriptor.getContentCategories();
    }

    @Override
    public boolean isInCategory(
            Object node,
            Object element,
            DiagramCategory category,
            DiagramBuilder builder) {

        return descriptor.isInCategory(node, element, category, builder);
    }


    @Override
    @SuppressWarnings({"removal"})
    public boolean isInCategory(Object element, DiagramCategory category, DiagramState state) {
        return descriptor.isInCategory(element, category, state);
    }
}
