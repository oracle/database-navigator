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

package com.dbn.connection.context;

import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseInterfacesProvider;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DatabaseContext extends DatabaseInterfacesProvider {

    @Nullable
    default Project getProject() {return null;}

    @Nullable
    ConnectionId getConnectionId();

    @Nullable
    SessionId getSessionId();

    @Nullable
    SchemaId getSchemaId();

    boolean isSameAs(DatabaseContext context);

    DBSchema getSchema();

    @Nullable
    String getSchemaName();

    @Nullable
    DatabaseSession getSession();

    @Nullable
    ConnectionHandler getConnection();

    @NotNull
    ConnectionHandler ensureConnection();

    @NotNull
    DBObjectBundle getObjectBundle();

    @NotNull
    DatabaseInterfaces getInterfaces();

    ConnectionContext createConnectionContext();
}
