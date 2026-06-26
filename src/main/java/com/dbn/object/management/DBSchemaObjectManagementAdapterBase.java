/*
 * Copyright 2026 Oracle and/or its affiliates
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
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.adapter.DBObjectDisableAdapter;
import com.dbn.object.management.adapter.DBObjectEnableAdapter;

import java.sql.SQLException;

import static com.dbn.object.event.ObjectChangeAction.DISABLE;
import static com.dbn.object.event.ObjectChangeAction.ENABLE;

public abstract class DBSchemaObjectManagementAdapterBase<T extends DBSchemaObject> extends ObjectManagementAdapterBase<T> {
    @Override
    public ObjectManagementAdapter<T> createAdapter(T object, ObjectChangeAction action) {
        return switch (action) {
            case ENABLE -> new DBObjectEnableAdapter<>(object, (d, c, o) -> enableObject(d, c, o));
            case DISABLE -> new DBObjectDisableAdapter<>(object, (d, c, o) -> disableObject(d, c, o));
            default -> super.createAdapter(object, action);
        };
    }

    protected void enableObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException {
        Exceptions.unsupported(ENABLE);
    }

    protected void disableObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException {
        Exceptions.unsupported(DISABLE);
    }
}
