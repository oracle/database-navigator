package com.dbn.object.diagram.impl;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.PRIVILEGE;
import static com.dbn.object.type.DBObjectType.ROLE;

public final class DBRoleModelDiagramProvider extends DBDiagramProvider<DBRole> {
    private static final DiagramCategory PRIVILEGES = createCategory(PRIVILEGE);
    private static final DiagramCategory ROLES = createCategory(ROLE);

    DBRoleModelDiagramProvider() {
        super(DBDiagramType.ROLE_MODEL);
    }

    @Override
    public DBDiagramInput<DBRole> createInput(DBRole source) {
        return new DBDiagramInput<>(this, source);
    }

    @Override
    public Collection<DBRole> getRootObjects(DBRole source) {
        List<DBRole> result = new ArrayList<>();
        Set<Object> visited = new HashSet<>();
        Deque<DBRole> pending = new ArrayDeque<>();
        pending.add(source);
        while (!pending.isEmpty()) {
            DBRole role = pending.removeFirst();
            if (!visited.add(role.ref())) continue;
            result.add(role);
            for (DBGrantedRole granted : role.getGrantedRoles()) {
                DBRole grantedRole = granted.getRole();
                if (grantedRole != null) pending.addLast(grantedRole);
            }
        }
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
                    result.add(new DBDiagramRelation<>(source, target,
                            txt("app.diagram.text.InheritsPrivilegesFrom", source.getName(), target.getName())));
            }
        return result;
    }

    @Override
    public DiagramCategory[] getContentCategories() {
        return new DiagramCategory[]{PRIVILEGES, ROLES};
    }

    @Override
    public boolean isInCategory(Object node, Object child, DiagramCategory category, DiagramBuilder builder) {
        if (PRIVILEGES.equals(category)) return child instanceof DBGrantedPrivilege;
        if (ROLES.equals(category)) return child instanceof DBGrantedRole;
        return false;
    }

    @Override
    public boolean isInCategory(Object child, DiagramCategory category, DiagramState state) {
        if (PRIVILEGES.equals(category)) return child instanceof DBGrantedPrivilege;
        if (ROLES.equals(category)) return child instanceof DBGrantedRole;
        return false;
    }
}
