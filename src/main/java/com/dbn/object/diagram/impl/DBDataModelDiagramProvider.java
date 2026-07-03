package com.dbn.object.diagram.impl;

import com.dbn.object.DBTable;
import com.dbn.object.diagram.model.DBDiagramDescriptor;
import com.dbn.object.diagram.model.DBDiagramProvider;
import com.intellij.diagram.DiagramCategory;

public final class DBDataModelDiagramProvider extends DBDiagramProvider<DBTable> {
    public static final String ID = "dbn_database_diagram";

    @Override
    public String getID() {
        return ID;
    }

    @Override
    protected DBDiagramDescriptor<DBTable> createDescriptor() {
        return new DBDataModelDiagramDescriptor();
    }

    @Override
    public String getPresentableName() {
        return "Database Diagram";
    }

    @Override
    public DiagramCategory[] getAllContentCategories() {
        return getDescriptor().getContentCategories();
    }
}
