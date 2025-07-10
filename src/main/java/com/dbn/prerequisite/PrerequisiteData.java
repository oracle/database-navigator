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

package com.dbn.prerequisite;

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.prerequisite.model.PrerequisiteBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Getter
public class PrerequisiteData {
    private final ConnectionRef connection;
    private final Map<DatabaseOperation, PrerequisiteBundle> prerequisites = new ConcurrentHashMap<>();

    public PrerequisiteData(ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Nullable
    public PrerequisiteBundle get(DatabaseOperation operation) {
        return prerequisites.get(operation);
    }

    public PrerequisiteBundle computeIfAbsent(DatabaseOperation operation, BiFunction<ConnectionHandler, DatabaseOperation, PrerequisiteBundle> supplier) {
        return prerequisites.computeIfAbsent(operation, k -> supplier.apply(getConnection(), operation));
    }
}
