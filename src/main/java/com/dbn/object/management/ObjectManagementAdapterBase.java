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

import com.dbn.common.exception.Exceptions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.adapter.DBObjectCreateAdapter;
import com.dbn.object.management.adapter.DBObjectDeleteAdapter;
import com.dbn.object.management.adapter.DBObjectUpdateAdapter;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.event.ObjectChangeAction.UPDATE;

public abstract class ObjectManagementAdapterBase<T extends DBObject> implements ObjectManagementAdapterExtension<T> {
    public abstract DBObjectType[] getObjectTypes();

    @Override
    public ObjectManagementAdapter<T> createAdapter(T object, ObjectChangeAction action) {
        return switch (action) {
            case CREATE -> new DBObjectCreateAdapter<>(object, (d, c, o) -> createObject(d, c, o));
            case UPDATE -> new DBObjectUpdateAdapter<>(object, (d, c, o) -> updateObject(d, c, o));
            case DELETE -> new DBObjectDeleteAdapter<>(object, (d, c, o) -> deleteObject(d, c, o));
            default -> Exceptions.unsupported(action);
        };
    }

    protected void createObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException {
        Exceptions.unsupported(CREATE);
    }

    protected void updateObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException {
        Exceptions.unsupported(UPDATE);
    }

    protected void deleteObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException {
        Exceptions.unsupported(DELETE);
    }
}
