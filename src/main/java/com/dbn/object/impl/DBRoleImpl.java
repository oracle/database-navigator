/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.object.impl;

import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBRoleMetadata;
import com.dbn.object.DBGrantedPrivilege;
import com.dbn.object.DBGrantedRole;
import com.dbn.object.DBPrivilege;
import com.dbn.object.DBRole;
import com.dbn.object.DBUser;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBRootObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.util.Lists.filter;

class DBRoleImpl extends DBRootObjectImpl<DBRoleMetadata> implements DBRole {

    public DBRoleImpl(ConnectionHandler connection, DBRoleMetadata metadata) throws SQLException {
        super(connection, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBRoleMetadata metadata) throws SQLException {
        return metadata.getRoleName();
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBObjectBundle objectBundle = connection.getObjectBundle();
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(DBObjectType.GRANTED_PRIVILEGE, this, objectBundle, DBObjectRelationType.ROLE_PRIVILEGE);
        childObjects.createSubcontentObjectList(DBObjectType.GRANTED_ROLE, this, objectBundle, DBObjectRelationType.ROLE_ROLE);
    }

    @Override
    protected void initProperties() {
        properties.set(DBObjectProperty.ROOT_OBJECT, true);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.ROLE;
    }

    @Override
    public List<DBGrantedPrivilege> getPrivileges() {
        return getChildObjects(DBObjectType.GRANTED_PRIVILEGE);
    }

    @Override
    public List<DBGrantedRole> getGrantedRoles() {
        return getChildObjects(DBObjectType.GRANTED_ROLE);
    }

    @Override
    public List<DBUser> getUserGrantees() {
        List<DBUser> users = getObjectBundle().getUsers();
        if (users == null || users.isEmpty()) return Collections.emptyList();
        return filter(users, u -> u.hasRole(this));
    }

    @Override
    public List<DBRole> getRoleGrantees() {
        List<DBRole> roles = getObjectBundle().getRoles();
        if (roles == null || roles.isEmpty()) return Collections.emptyList();
        return filter(roles, r -> r.hasRole(this));
    }

    @Override
    public boolean hasPrivilege(DBPrivilege privilege) {
        for (DBGrantedPrivilege rolePrivilege : getPrivileges()) {
            if (Objects.equals(rolePrivilege.getPrivilege(), privilege)) {
                return true;
            }
        }
        for (DBGrantedRole inheritedRole : getGrantedRoles()) {
            if (inheritedRole.hasPrivilege(privilege)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasRole(DBRole role) {
        for (DBGrantedRole inheritedRole : getGrantedRoles()) {
            if (Objects.equals(inheritedRole.getRole(), role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();
        navigationLists.add(DBObjectNavigationList.create("User grantees", getUserGrantees()));

        if (DBObjectType.ROLE.isSupported(this)) {
            navigationLists.add(DBObjectNavigationList.create("Role grantees", getRoleGrantees()));
        }
        return navigationLists;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return DatabaseBrowserUtils.createList(
                getChildObjectList(DBObjectType.GRANTED_PRIVILEGE),
                getChildObjectList(DBObjectType.GRANTED_ROLE));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(DBObjectType.PRIVILEGE) ||
            settings.isVisible(DBObjectType.ROLE);
    }
}
