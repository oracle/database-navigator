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
import com.dbn.database.common.metadata.def.DBPrivilegeMetadata;
import com.dbn.object.DBPrivilege;
import com.dbn.object.DBRole;
import com.dbn.object.DBUser;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBRootObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static com.dbn.object.type.DBObjectType.ROLE;
import static com.dbn.object.type.DBObjectType.USER;

abstract class DBPrivilegeImpl<M extends DBPrivilegeMetadata> extends DBRootObjectImpl<M> implements DBPrivilege {

    DBPrivilegeImpl(ConnectionHandler connection, M metadata) throws SQLException {
        super(connection, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, M metadata) throws SQLException {
        return metadata.getPrivilegeName();
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(USER, this, getObjectBundle(), USER);
        childObjects.createSubcontentObjectList(ROLE, this, getObjectBundle(), ROLE);
    }

    @Override
    public List<DBUser> getUserGrantees() {
        return getChildObjects(USER);
    }

    public List<DBRole> getRoleGrantees() {
        return getChildObjects(ROLE);
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();
        navigationLists.add(DBObjectNavigationList.create("User grantees", getUserGrantees()));

        if (ROLE.isSupported(this)) {
            navigationLists.add(DBObjectNavigationList.create("Role grantees", getRoleGrantees()));
        }
        return navigationLists;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    public boolean isLeaf() {
        return true;
    }
}
