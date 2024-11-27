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

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBGrantedRoleMetadata;
import com.dbn.object.DBGrantedRole;
import com.dbn.object.DBPrivilege;
import com.dbn.object.DBRole;
import com.dbn.object.DBRoleGrantee;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

class DBGrantedRoleImpl extends DBObjectImpl<DBGrantedRoleMetadata> implements DBGrantedRole {
    private DBObjectRef<DBRole> role;

    public DBGrantedRoleImpl(DBRoleGrantee grantee, DBGrantedRoleMetadata metadata) throws SQLException {
        super(grantee, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBGrantedRoleMetadata metadata) throws SQLException {
        String name = metadata.getGrantedRoleName();
        this.role = DBObjectRef.of(connection.getObjectBundle().getRole(name));
        set(DBObjectProperty.ADMIN_OPTION, metadata.isAdminOption());
        set(DBObjectProperty.DEFAULT_ROLE, metadata.isDefaultRole());
        return name;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.GRANTED_ROLE;
    }

    @Override
    public DBRoleGrantee getGrantee() {
        return (DBRoleGrantee) getParentObject();
    }

    @Override
    public DBRole getRole() {
        return DBObjectRef.get(role);
    }

    @Override
    public boolean isAdminOption() {
        return is(DBObjectProperty.ADMIN_OPTION);
    }

    @Override
    public boolean isDefaultRole() {
        return is(DBObjectProperty.DEFAULT_ROLE);
    }

    @Override
    public boolean hasPrivilege(DBPrivilege privilege) {
        DBRole role = getRole();
        return role != null && role.hasPrivilege(privilege);
    }

    @Nullable
    @Override
    public DBObject getDefaultNavigationObject() {
        return getRole();
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    public boolean isLeaf() {
        return true;
    }


}
