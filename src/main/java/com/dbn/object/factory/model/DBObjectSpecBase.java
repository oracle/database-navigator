/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.factory.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class DBObjectSpecBase {
    private final DBObjectSpec parent;
    private final DatabaseEntity parentEntity;
    private ConnectionId connectionId;
    private SchemaId schemaId;

    public DBObjectSpecBase(@Nullable DBObjectSpec parent) {
        this.parent = parent;
        this.parentEntity = null;
    }

    public DBObjectSpecBase(DatabaseEntity parentEntity) {
        this.parent = null;
        this.parentEntity = parentEntity;
    }

    public ConnectionHandler getConnection() {
        if (parent != null) return parent.getConnection();
        if (parentEntity != null) return parentEntity.getConnection();

        return ConnectionHandler.ensure(connectionId);
    }

    public DBSchema getSchema() {
        if (parent != null) return parent.getSchema();
        if (parentEntity != null) return parentEntity.getSchema();

        return getConnection().getSchema(schemaId);
    }

    public Project getProject() {
        return getConnection().getProject();
    }

    public String getSchemaName() {
        return getSchema().getName();
    }

    public String getSchemaName(boolean quoted) {
        return getSchema().getName(quoted);
    }
}
