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
    private final DBDiagramProvider<?> provider;

    DBDiagramNodeContentManager(DBDiagramProvider<?> provider) {
        this.provider = provider;
        for (DiagramCategory category : provider.getContentCategories()) setCategoryEnabled(category, true);
    }

    @Override
    public DiagramCategory[] getContentCategories() {
        return provider.getContentCategories();
    }

    @Override
    public boolean isInCategory(
            Object node,
            Object element,
            DiagramCategory category,
            DiagramBuilder builder) {

        return provider.isInCategory(node, element, category, builder);
    }


    @Override
    @SuppressWarnings({"removal"})
    public boolean isInCategory(Object element, DiagramCategory category, DiagramState state) {
        return provider.isInCategory(element, category, state);
    }
}
