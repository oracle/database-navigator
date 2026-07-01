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

package com.dbn.database;

import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseInterfacesBundle;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DatabaseFeature {
    OBJECT_REPLACING(txt("app.database.const.DatabaseFeature_OBJECT_REPLACING")),
    OBJECT_DEPENDENCIES(txt("app.database.const.DatabaseFeature_OBJECT_DEPENDENCIES")),
    OBJECT_DDL_EXTRACTION(txt("app.database.const.DatabaseFeature_OBJECT_DDL_EXTRACTION")),
    OBJECT_INVALIDATION(txt("app.database.const.DatabaseFeature_OBJECT_INVALIDATION")),
    OBJECT_DISABLING(txt("app.database.const.DatabaseFeature_OBJECT_DISABLING")),
    OBJECT_SOURCE_EDITING(txt("app.database.const.DatabaseFeature_OBJECT_SOURCE_EDITING")),
    OBJECT_CHANGE_MONITORING(txt("app.database.const.DatabaseFeature_OBJECT_CHANGE_MONITORING")),
    AUTHID_METHOD_EXECUTION(txt("app.database.const.DatabaseFeature_AUTHID_METHOD_EXECUTION")),
    FUNCTION_OUT_ARGUMENTS(txt("app.database.const.DatabaseFeature_FUNCTION_OUT_ARGUMENTS")),
    DEBUGGING(txt("app.database.const.DatabaseFeature_DEBUGGING")),
    EXPLAIN_PLAN(txt("app.database.const.DatabaseFeature_EXPLAIN_PLAN")),
    DATABASE_LOGGING(txt("app.database.const.DatabaseFeature_DATABASE_LOGGING")),
    SESSION_CURRENT_SQL(txt("app.database.const.DatabaseFeature_SESSION_CURRENT_SQL")),
    SESSION_BROWSING(txt("app.database.const.DatabaseFeature_SESSION_BROWSING")),
    SESSION_KILL(txt("app.database.const.DatabaseFeature_SESSION_KILL")),
    SESSION_DISCONNECT(txt("app.database.const.DatabaseFeature_SESSION_DISCONNECT")),
    SESSION_INTERRUPTION_TIMING(txt("app.database.const.DatabaseFeature_SESSION_INTERRUPTION_TIMING")),
    CONNECTION_ERROR_RECOVERY(txt("app.database.const.DatabaseFeature_CONNECTION_ERROR_RECOVERY")),
    UPDATABLE_RESULT_SETS(txt("app.database.const.DatabaseFeature_UPDATABLE_RESULT_SETS")),
    CURRENT_SCHEMA(txt("app.database.const.DatabaseFeature_CURRENT_SCHEMA")),
    USER_SCHEMA(txt("app.database.const.DatabaseFeature_USER_SCHEMA")),
    CONSTRAINT_MANIPULATION(txt("app.database.const.DatabaseFeature_CONSTRAINT_MANIPULATION")),
    READONLY_CONNECTIVITY(txt("app.database.const.DatabaseFeature_READONLY_CONNECTIVITY")),
    AI_ASSISTANT(txt("app.database.const.DatabaseFeature_AI_ASSISTANT")),
    DATA_CHANGE_NOTIFICATION(txt("app.database.const.DatabaseFeature_DATA_CHANGE_NOTIFICATION")),
    VECTOR_EMBEDDING(txt("app.database.const.DatabaseFeature_VECTOR_EMBEDDING")),
    VECTOR_SEARCH(txt("app.database.const.DatabaseFeature_VECTOR_SEARCH")),
    MCP_SERVER_BUILDER(txt("app.database.const.DatabaseFeature_MCP_SERVER_BUILDER")),
    CONNECTION_CONFIGURATION("Connection configurations"),

    // OJVM
    JAVA_VIRTUAL_MACHINE(txt("app.database.const.DatabaseFeature_JAVA_VIRTUAL_MACHINE")),

    @Deprecated // temporary disabled feature because of performance issues with empty schema evaluations
    EMPTY_SCHEMA_EVALUATION(txt("app.database.const.DatabaseFeature_EMPTY_SCHEMA_EVALUATION")),
    ;

    private final @Nls String description;

    DatabaseFeature(@Nls String description) {
        this.description = description;
    }

    public boolean isNotSupported(@Nullable DatabaseContext context) {
        return !isSupported(context);
    }

    public boolean isSupported(@Nullable DatabaseContext context) {
        if (context == null) return false;

        DatabaseCompatibilityInterface compatibility = context.getCompatibilityInterface();
        if (context instanceof DBObject object) {
            // qualified feature support lookup
            DatabaseObjectTypeId objectTypeId = object.getObjectType().getTypeId();
            return compatibility.supportsFeature(this, objectTypeId);
        }

        return compatibility.supportsFeature(this);
    }

    public boolean isSupported(DatabaseType databaseType) {
        DatabaseInterfaces databaseInterfaces = DatabaseInterfacesBundle.get(databaseType);
        DatabaseCompatibilityInterface compatibility = databaseInterfaces.getCompatibilityInterface();
        return compatibility != null && compatibility.supportsFeature(this);
    }

    public boolean isSupported(Project project) {
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        return connectionBundle.hasConnections(this);
    }
}
