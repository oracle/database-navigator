package com.dbn.object.diagram.impl;

import com.dbn.common.icon.Icons;
import com.dbn.object.DBGrantedPrivilege;
import com.dbn.object.DBGrantedRole;
import com.dbn.object.DBRole;
import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.model.DBDiagramInput;
import com.dbn.object.diagram.model.DBDiagramProvider;
import com.dbn.object.diagram.model.DBDiagramRelation;
import com.dbn.object.diagram.model.DBDiagramType;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DBRoleDiagramProvider extends DBDiagramProvider<DBRole> {
    private static final DiagramCategory PRIVILEGES = new DiagramCategory(() -> "Privileges", Icons.DBO_PRIVILEGES);
    private static final DiagramCategory GRANTED_ROLES = new DiagramCategory(() -> "Granted Roles", Icons.DBO_ROLES);

    DBRoleDiagramProvider() {
        super(DBDiagramType.ROLE_MODEL);
    }

    @Override
    public DBDiagramInput<DBRole> createInput(DBRole source) {
        return new DBDiagramInput<>(this, source);
    }

    @Override
    public Collection<DBRole> getRootObjects(DBRole source) {
        List<DBRole> result = new ArrayList<>();
        result.add(source);
        for (DBGrantedRole granted : source.getGrantedRoles())
            if (granted.getRole() != null && !result.contains(granted.getRole())) result.add(granted.getRole());
        return result;
    }

    @Override
    public Collection<? extends DBObject> getChildObjects(DBRole root) {
        List<DBObject> result = new ArrayList<>();
        result.addAll(root.getPrivileges());
        result.addAll(root.getGrantedRoles());
        return result;
    }

    @Override
    public Collection<DBDiagramRelation<DBRole>> getRelations(Collection<? extends DBRole> roots) {
        List<DBDiagramRelation<DBRole>> result = new ArrayList<>();
        for (DBRole source : roots)
            for (DBGrantedRole granted : source.getGrantedRoles()) {
                DBRole target = granted.getRole();
                if (target != null && roots.contains(target))
                    result.add(new DBDiagramRelation<>(source, target, "GRANTS"));
            }
        return result;
    }

    @Override
    public DiagramCategory[] getContentCategories() {
        return new DiagramCategory[]{PRIVILEGES, GRANTED_ROLES};
    }

    @Override
    public boolean isInCategory(Object node, Object child, DiagramCategory category, DiagramBuilder builder) {
        if (PRIVILEGES.equals(category)) return child instanceof DBGrantedPrivilege;
        if (GRANTED_ROLES.equals(category)) return child instanceof DBGrantedRole;
        return false;
    }

    @Override
    public boolean isInCategory(Object child, DiagramCategory category, DiagramState state) {
        if (PRIVILEGES.equals(category)) return child instanceof DBGrantedPrivilege;
        if (GRANTED_ROLES.equals(category)) return child instanceof DBGrantedRole;
        return false;
    }
}
