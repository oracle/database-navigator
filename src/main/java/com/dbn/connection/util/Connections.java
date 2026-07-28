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

package com.dbn.connection.util;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@UtilityClass
public final class Connections {

    @Nullable
    public static ConnectionHandler get(@Nullable ConnectionId connectionId) {
        return connectionId == null ? null : ConnectionHandler.get(connectionId);
    }

    @NotNull
    public static String getName(@Nullable ConnectionId connectionId) {
        ConnectionHandler connection = get(connectionId);
        return connection == null ? Objects.toString(connectionId, "Unknown connection") : connection.getName();
    }

    @Nullable
    public static DatabaseType getDatabaseType(@Nullable ConnectionId connectionId) {
        ConnectionHandler connection = get(connectionId);
        return connection == null ? null : connection.getDatabaseType();
    }
}
