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

package com.dbn.connection;

import com.dbn.common.Reference;
import com.dbn.common.dispose.Checks;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.index.Identifiable;
import com.dbn.common.ref.WeakRef;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@EqualsAndHashCode
public final class ConnectionRef implements Reference<ConnectionHandler>, Identifiable<ConnectionId> {
    private static final Map<ConnectionId, ConnectionRef> registry = new ConcurrentHashMap<>();
    private final ConnectionId connectionId;

    private transient volatile boolean resolving;
    private transient WeakRef<ConnectionHandler> reference;

    private ConnectionRef(ConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public ConnectionId getId() {
        return connectionId;
    }

    @NotNull
    public ConnectionHandler ensure() {
        ConnectionHandler connection = get();
        return Failsafe.nn(connection);
    }


    @Nullable
    public ConnectionHandler get() {
        if (connectionId != null && !isValid()) {
            if (!resolving) {
                synchronized (this) {
                    if (!resolving) {
                        try {
                            resolving = true;
                            ConnectionHandler connection = ConnectionHandler.get(connectionId);
                            reference = WeakRef.of(connection);
                        } finally {
                            resolving = false;
                        }
                    }
                }
            }
        }
        return reference();
    }

    public boolean isValid() {
        return Checks.isValid(reference());
    }

    @Nullable
    private ConnectionHandler reference() {
        return reference == null ? null : reference.get();
    }

    /**************************************************************************
     *                         Static utilities                               *
     **************************************************************************/


    @Contract("null -> null;!null -> !null;")
    public static ConnectionRef of(@Nullable ConnectionHandler connection) {
        if (connection != null) {
            ConnectionRef ref = ConnectionRef.of(connection.getConnectionId());
            ConnectionHandler local = ref.reference();
            if (local == null || local != connection) {
                ref.reference = WeakRef.of(connection);
            }
            return ref;
        }
        return null;
    }

    @NotNull
    public static ConnectionRef of(@NotNull ConnectionId connectionId) {
        return registry.computeIfAbsent(connectionId, id -> new ConnectionRef(id));
    }

    @Contract("null -> null;!null -> !null;")
    public static ConnectionHandler get(@Nullable ConnectionRef ref) {
        return ref == null ? null : ref.get();
    }

    @NotNull
    public static ConnectionHandler ensure(@NotNull ConnectionRef ref) {
        return Failsafe.nn(ref).ensure();
    }

    @Override
    public String toString() {
        ConnectionHandler connection = get();
        return connection == null ? Objects.toString(connectionId) : connection.getName();
    }
}
