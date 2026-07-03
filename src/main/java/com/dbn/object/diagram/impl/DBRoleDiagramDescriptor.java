package com.dbn.object.diagram.impl;

import com.dbn.object.DBGrantedPrivilege;
import com.dbn.object.DBGrantedRole;
import com.dbn.object.DBRole;
import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.model.DBDiagramDescriptor;
import com.dbn.object.diagram.model.DBDiagramInput;
import com.dbn.object.diagram.model.DBDiagramRelation;
import com.dbn.object.diagram.model.DBDiagramType;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class DBRoleDiagramDescriptor implements DBDiagramDescriptor<DBRole> {
    private static final DiagramCategory PRIVILEGES = new DiagramCategory(() -> "Privileges", null);

    @Override
    public DBDiagramType getDiagramType() {
        return DBDiagramType.ROLE_MODEL;
    }

    @Override
    public DBDiagramInput<DBRole> createInput(DBRole role) {
        return new DBDiagramInput<>(this, role);
    }

    @Override
    public Collection<DBRole> getRootObjects(DBRole role) {
        List<DBRole> roots = new ArrayList<>();
        for (DBGrantedRole grantedRole : role.getGrantedRoles()) {
            DBRole granted = grantedRole.getRole();
            if (granted != null) roots.add(granted);
        }
        return roots;
    }

    @Override
    public Collection<? extends DBObject> getChildObjects(DBRole role) {
        return role.getPrivileges();
    }

    @Override
    public Collection<DBDiagramRelation<DBRole>> getRelations(Collection<? extends DBRole> roots) {
        return Collections.emptyList();
    }

    @Override
    public DiagramCategory[] getContentCategories() {
        return new DiagramCategory[]{PRIVILEGES};
    }

    @Override
    public boolean isInCategory(Object node, Object child, DiagramCategory category, DiagramBuilder builder) {
        return PRIVILEGES.equals(category) && child instanceof DBGrantedPrivilege;
    }

    @Override
    public boolean isInCategory(Object child, DiagramCategory category, DiagramState state) {
        return PRIVILEGES.equals(category) && child instanceof DBGrantedPrivilege;
    }
}
