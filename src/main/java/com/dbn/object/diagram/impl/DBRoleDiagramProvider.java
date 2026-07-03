package com.dbn.object.diagram.impl;

import com.dbn.object.diagram.model.DBDiagramDescriptor;
import com.dbn.object.diagram.model.DBDiagramProvider;
import com.dbn.object.diagram.model.DBDiagramType;

public final class DBRoleDiagramProvider extends DBDiagramProvider {
    DBRoleDiagramProvider() {
        super(DBDiagramType.ROLE_MODEL);
    }

    @Override
    protected DBDiagramDescriptor<?> createDescriptor() {
        return new DBRoleDiagramDescriptor();
    }

}
