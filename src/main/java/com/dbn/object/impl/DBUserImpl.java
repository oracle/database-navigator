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
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBUserMetadata;
import com.dbn.object.DBGrantedPrivilege;
import com.dbn.object.DBGrantedRole;
import com.dbn.object.DBPrivilege;
import com.dbn.object.DBRole;
import com.dbn.object.DBSchema;
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

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class DBUserImpl extends DBRootObjectImpl<DBUserMetadata> implements DBUser {

    DBUserImpl(ConnectionHandler connection, DBUserMetadata metadata) throws SQLException {
        super(connection, metadata);
    }

    @Nullable
    @Override
    public DBUser getOwner() {
        return this;
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBUserMetadata metadata) throws SQLException {
        String name = metadata.getUserName();
        set(DBObjectProperty.EXPIRED, metadata.isExpired());
        set(DBObjectProperty.LOCKED, metadata.isLocked());
        set(DBObjectProperty.SESSION_USER, Strings.equalsIgnoreCase(name, connection.getUserName()));
        return name;
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBObjectListContainer childObjects = ensureChildObjects();
        DBObjectBundle objectBundle = getObjectBundle();
        childObjects.createSubcontentObjectList(DBObjectType.GRANTED_ROLE, this, objectBundle, DBObjectRelationType.USER_ROLE);
        childObjects.createSubcontentObjectList(DBObjectType.GRANTED_PRIVILEGE, this, objectBundle, DBObjectRelationType.USER_PRIVILEGE);
    }

    @Override
    protected void initProperties() {
        properties.set(DBObjectProperty.ROOT_OBJECT, true);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.USER;
    }

    @Override
    public DBSchema getSchema() {
        return getObjectBundle().getSchema(getName());
    }

    @Override
    public boolean isExpired() {
        return is(DBObjectProperty.EXPIRED);
    }

    @Override
    public boolean isLocked() {
        return is(DBObjectProperty.LOCKED);
    }

    @Override
    public boolean isSessionUser() {
        return is(DBObjectProperty.SESSION_USER);
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return isExpired() ?
               (isLocked() ? Icons.DBO_USER_EXPIRED_LOCKED : Icons.DBO_USER_EXPIRED) :
               (isLocked() ? Icons.DBO_USER_LOCKED : Icons.DBO_USER);
    }

    @Nullable
    @Override
    public DBObject getDefaultNavigationObject() {
        return getSchema();
    }

    @Override
    public List<DBGrantedPrivilege> getPrivileges() {
        return getChildObjects(DBObjectType.GRANTED_PRIVILEGE);
    }

    @Override
    public List<DBGrantedRole> getRoles() {
        return getChildObjects(DBObjectType.GRANTED_ROLE);
    }

    @Override
    public boolean hasPrivilege(DBPrivilege privilege) {
        for (DBGrantedPrivilege grantedPrivilege : getPrivileges()) {
            if (Objects.equals(grantedPrivilege.getPrivilege(), privilege)) {
                return true;
            }
        }
        if (DBObjectType.GRANTED_ROLE.isSupported(this)) {
            for (DBGrantedRole grantedRole : getRoles()) {
                if (grantedRole.hasPrivilege(privilege)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasRole(DBRole role) {
        for (DBGrantedRole grantedRole : getRoles()) {
            if (Objects.equals(grantedRole.getRole(), role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        if (isLocked() || isExpired()) {
            if (isLocked() && isExpired())
                ttb.append(false, " - expired & locked" , true);
            else if (isLocked())
                ttb.append(false, " - locked" , true); else
                ttb.append(false, " - expired" , true);


        }

        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        DBSchema schema = getSchema();
        if(schema != null) {
            List<DBObjectNavigationList> navigationLists = new LinkedList<>();
            navigationLists.add(DBObjectNavigationList.create("Schema", schema));
            return navigationLists;
        }
        return null;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return DatabaseBrowserUtils.createList(
                getChildObjectList(DBObjectType.GRANTED_ROLE),
                getChildObjectList(DBObjectType.GRANTED_PRIVILEGE));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getConnection().getSettings().getFilterSettings().getObjectTypeFilterSettings();
        return
            settings.isVisible(DBObjectType.ROLE) ||
            settings.isVisible(DBObjectType.PRIVILEGE);
    }
}
