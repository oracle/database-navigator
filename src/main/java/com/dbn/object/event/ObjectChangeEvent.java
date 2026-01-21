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

package com.dbn.object.event;

import com.dbn.common.event.ProjectEvents;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SchemaId;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

import static com.dbn.common.util.Unsafe.cast;

@Getter
public class ObjectChangeEvent {
    private final ObjectChangeAction changeAction;
    private final DBObjectType objectType;
    private final ConnectionId connectionId;
    private final SchemaId ownerId;
    private DBObjectRef<?> object;

    private ObjectChangeEvent(ObjectChangeAction changeAction, DBObjectType objectType, ConnectionId connectionId, SchemaId ownerId) {
        this.changeAction = changeAction;
        this.connectionId = connectionId;
        this.objectType = objectType;
        this.ownerId = ownerId;
    }

    private ObjectChangeEvent(ObjectChangeAction changeAction, DBObject object) {
        this(changeAction,
            object.getObjectType(),
            object.getConnectionId(),
            object.getSchemaId());

        this.object = DBObjectRef.of(object);
    }

    public boolean matches(ConnectionHandler connection) {
        return connection != null && matches(connection.getConnectionId());
    }

    public boolean matches(ConnectionRef connection) {
        return connection != null && matches(connection.getConnectionId());
    }

    public boolean matches(ConnectionId connectionId) {
        return Objects.equals(this.connectionId, connectionId);
    }

    public boolean matches(SchemaId schemaId) {
        return Objects.equals(this.ownerId, schemaId);
    }

    public boolean matches(DBObjectType ... objectTypes) {
        return this.objectType.isOneOf(objectTypes);
    }

    public boolean matches(ObjectChangeAction... actions) {
        return this.changeAction.isOneOf(actions);
    }

    public static ObjectChangeEvent create(ObjectChangeAction action, DBObjectType objectType, ConnectionId connectionId, SchemaId ownerId) {
        return new ObjectChangeEvent(action, objectType, connectionId, ownerId);
    }

    private static ObjectChangeEvent create(ObjectChangeAction action, DBObject object) {
        return new ObjectChangeEvent(action, object);
    }

    public static void notify(ObjectChangeAction action, DBObjectType objectType, ConnectionId connectionId, SchemaId ownerId) {
        ObjectChangeEvent event = create(action, objectType, connectionId, ownerId);
        event.notifyEvent();
    }

    public static void notify(ObjectChangeAction action, DBObject object) {
        ObjectChangeEvent event = create(action, object);
        event.notifyEvent();
    }

    private void notifyEvent() {
        ConnectionHandler connection = getConnection();
        if (connection == null) return;

        Project project = connection.getProject();
        ProjectEvents.notify(project, ObjectChangeListener.TOPIC, l -> l.objectsChanged(this));
    }

    public static void subscribe(
            Project project,
            Disposable parentDisposable,
            Supplier<ConnectionId> connectionId,
            Supplier<SchemaId> ownerId,
            Supplier<DBObjectType> objectType, Runnable runnable) {
        if (project == null) return;

        ProjectEvents.subscribe(project, parentDisposable, ObjectChangeListener.TOPIC, e -> {
            if (!e.matches(connectionId.get())) return;
            if (!e.matches(ownerId.get())) return;
            if (!e.matches(objectType.get())) return;
            runnable.run();
        });
    }

    @Nullable
    private ConnectionHandler getConnection() {
        return ConnectionHandler.get(connectionId);
    }

    @Nullable
    public <T extends DBObject> T getObject() {
        return cast(DBObjectRef.get(object));
    }
}
