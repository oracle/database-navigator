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

package com.dbn.migration.task;

import com.dbn.common.component.ProjectUnit;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Common database context required by a migration task.
 *
 * <p>Engine-specific inputs extend this class with their workspace, policy, and operation
 * settings. The common layer keeps the project, connection, and relevant schema context
 * available to the execution console without imposing a particular migration file model.</p>
 */
public abstract class DatabaseMigrationTaskInput extends ProjectUnit {
    protected DatabaseMigrationTaskInput(@NotNull Project project) {
        super(project);
    }

    @NotNull
    public abstract ConnectionHandler getRelevantConnection();

    @NotNull
    public abstract DBSchema getRelevantSchema();

    @NotNull
    public ConnectionId getRelevantConnectionId() {
        return getRelevantConnection().getConnectionId();
    }

    @NotNull
    public SchemaId getRelevantSchemaId() {
        return getRelevantSchema().getSchemaId();
    }
}
