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
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.metadata.def.DBUserMetadata;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
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
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.util.Strings.equalsIgnoreCase;
import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.LOCKABLE;
import static com.dbn.object.common.property.DBObjectProperty.ROOT_OBJECT;
import static com.dbn.object.common.property.DBObjectProperty.SESSION_USER;
import static com.dbn.object.common.property.DBObjectProperty.SYSTEM_OBJECT;
import static com.dbn.object.common.status.DBObjectStatus.DISABLED;
import static com.dbn.object.common.status.DBObjectStatus.EXPIRED;
import static com.dbn.object.common.status.DBObjectStatus.LOCKED;
import static com.dbn.object.event.ObjectChangeAction.DISABLE;
import static com.dbn.object.event.ObjectChangeAction.LOCK;

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
        set(SYSTEM_OBJECT, metadata.isSystem());
        set(SESSION_USER, equalsIgnoreCase(name, connection.getUserName()));
        setStatus(EXPIRED, metadata.isExpired());
        setStatus(DISABLED, metadata.isDisabled());
        setStatus(LOCKED, metadata.isLocked());
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
        properties.set(ROOT_OBJECT, true);

        DatabaseCompatibilityInterface compatibilityInterface = getConnection().getCompatibilityInterface();
        DatabaseObjectTypeId objectTypeId = getObjectType().getTypeId();
        properties.set(DISABLEABLE, compatibilityInterface.supportsObjectAction(objectTypeId, DISABLE));
        properties.set(LOCKABLE, compatibilityInterface.supportsObjectAction( objectTypeId, LOCK));
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
        return hasStatus(EXPIRED);
    }

    @Override
    public boolean isSystemUser() {
        return is(SYSTEM_OBJECT);
    }

    @Override
    public boolean isLocked() {
        return hasStatus(LOCKED);
    }

    @Override
    public boolean isDisabled() {
        return hasStatus(DISABLED);
    }

    @Override
    public boolean isSessionUser() {
        return is(SESSION_USER);
    }

    @Nullable
    @Override
    public Icon getIcon() {
        boolean expired = isExpired() || isDisabled();
        boolean locked = isLocked();
        return expired ?
               (locked ? Icons.DBO_USER_EXPIRED_LOCKED : Icons.DBO_USER_EXPIRED) :
               (locked ? Icons.DBO_USER_LOCKED : Icons.DBO_USER);
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
