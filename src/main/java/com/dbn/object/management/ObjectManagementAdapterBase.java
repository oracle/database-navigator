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

package com.dbn.object.management;

import com.dbn.common.attributes.Attributes;
import com.dbn.common.exception.Exceptions;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.adapter.DBObjectCreateAdapter;
import com.dbn.object.management.adapter.DBObjectDeleteAdapter;
import com.dbn.object.management.adapter.DBObjectDisableAdapter;
import com.dbn.object.management.adapter.DBObjectEnableAdapter;
import com.dbn.object.management.adapter.DBObjectLockAdapter;
import com.dbn.object.management.adapter.DBObjectRefreshAdapter;
import com.dbn.object.management.adapter.DBObjectRenameAdapter;
import com.dbn.object.management.adapter.DBObjectUnlockAdapter;
import com.dbn.object.management.adapter.DBObjectUpdateAdapter;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.event.ObjectChangeAction.DISABLE;
import static com.dbn.object.event.ObjectChangeAction.ENABLE;
import static com.dbn.object.event.ObjectChangeAction.LOCK;
import static com.dbn.object.event.ObjectChangeAction.REFRESH;
import static com.dbn.object.event.ObjectChangeAction.RENAME;
import static com.dbn.object.event.ObjectChangeAction.UNLOCK;
import static com.dbn.object.event.ObjectChangeAction.UPDATE;

public abstract class ObjectManagementAdapterBase<T extends DBObject> implements ObjectManagementAdapterExtension<T> {
    public abstract DBObjectType[] getObjectTypes();

    @Override
    public ObjectManagementAdapter<T> createAdapter(T object, ObjectChangeAction action, Attributes attributes) {
        return switch (action) {
            case CREATE  -> new DBObjectCreateAdapter<> (object, attributes, r -> createObject(r));
            case UPDATE  -> new DBObjectUpdateAdapter<> (object, attributes, r -> updateObject(r));
            case DELETE  -> new DBObjectDeleteAdapter<> (object, attributes, r -> deleteObject(r));
            case ENABLE  -> new DBObjectEnableAdapter<> (object, attributes, r -> enableObject(r));
            case DISABLE -> new DBObjectDisableAdapter<>(object, attributes, r -> disableObject(r));
            case REFRESH -> new DBObjectRefreshAdapter<>(object, attributes, r -> refreshObject(r));
            case RENAME  -> new DBObjectRenameAdapter<> (object, attributes, r -> renameObject(r));
            case LOCK    -> new DBObjectLockAdapter<>   (object, attributes, r -> lockObject(r));
            case UNLOCK  -> new DBObjectUnlockAdapter<> (object, attributes, r -> unlockObject(r));
            default -> Exceptions.unsupported(action);
        };
    }

    protected void renameObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(RENAME);
    }

    protected void createObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(CREATE);
    }

    protected void updateObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(UPDATE);
    }

    protected void deleteObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(DELETE);
    }

    protected void enableObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(ENABLE);
    }

    protected void disableObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(DISABLE);
    }

    protected void lockObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(LOCK);
    }

    protected void unlockObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(UNLOCK);
    }

    protected void refreshObject(ObjectManagementRequest<T> request) throws SQLException {
        Exceptions.unsupported(REFRESH);
    }
}
