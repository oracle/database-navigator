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

package com.dbn.object.common;

import com.dbn.common.dispose.Disposer;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

@Getter
public abstract class DBRootObjectImpl<M extends DBObjectMetadata> extends DBObjectImpl<M> implements DBRootObject {

    private volatile DBObjectListContainer childObjects;

    protected DBRootObjectImpl(@NotNull ConnectionHandler connection, M metadata) throws SQLException {
        super(connection, metadata);
    }

    protected DBRootObjectImpl(@Nullable ConnectionHandler connection, DBObjectType objectType, String name) {
        super(connection, objectType, name);
    }

    protected DBRootObjectImpl(@NotNull DBObject parentObject, M metadata) throws SQLException {
        super(parentObject, metadata);
    }

    @Override
    protected void init(ConnectionHandler connection, DBObject parentObject, M metadata) throws SQLException {
        super.init(connection, parentObject, metadata);
        initLists(connection);
    }

    @NotNull
    protected DBObjectListContainer ensureChildObjects() {
        if (childObjects == null) {
            synchronized (this) {
                if (childObjects == null) {
                    childObjects = new DBObjectListContainer(this);
                }
            }
        }
        return childObjects;
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
        Disposer.dispose(childObjects);
    }
}
