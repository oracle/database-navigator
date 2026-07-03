package com.dbn.object.diagram.impl;

import com.dbn.object.DBTable;
import com.dbn.object.diagram.model.DBDiagramDescriptor;
import com.dbn.object.diagram.model.DBDiagramProvider;
import com.dbn.object.diagram.model.DBDiagramType;

public final class DBDataModelDiagramProvider extends DBDiagramProvider<DBTable> {

    DBDataModelDiagramProvider() {
        super(DBDiagramType.DATA_MODEL);
    }

    @Override
    protected DBDiagramDescriptor<DBTable> createDescriptor() {
        return new DBDataModelDiagramDescriptor();
    }
}
