/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.object.diagram.model;

import com.dbn.object.DBColumn;
import com.intellij.diagram.AbstractDiagramNodeContentManager;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;

final class DBNDiagramNodeContentManager extends AbstractDiagramNodeContentManager {
    private static final DiagramCategory COLUMNS = new DiagramCategory(() -> "Columns", null);

    DBNDiagramNodeContentManager() {
        setCategoryEnabled(COLUMNS, true);
    }

    @Override
    public DiagramCategory[] getContentCategories() {
        return new DiagramCategory[]{COLUMNS};
    }

    @Override
    public boolean isInCategory(
            Object node,
            Object element,
            DiagramCategory category,
            DiagramBuilder builder) {

        return COLUMNS.equals(category) && element instanceof DBColumn;
    }


    @Override
    @SuppressWarnings({"removal"})
    public boolean isInCategory(Object element, DiagramCategory category, DiagramState state) {
        return COLUMNS.equals(category) && element instanceof DBColumn;
    }
}
