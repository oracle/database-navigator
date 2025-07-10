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

package com.dbn.common.operation;

import lombok.Data;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;

/**
 * Represents a database operation, capturing its type and allowing the storage
 * of arbitrary attributes related to the operation.
 */
@Data // used as key in HashSet
public class DatabaseOperation {
    private final DatabaseOperationType type;
    private final Map<String, Object> attributes = new HashMap<>();

    public DatabaseOperation(DatabaseOperationType type) {
        this.type = type;
    }

    public static DatabaseOperation create(DatabaseOperationType type) {
        return new DatabaseOperation(type);
    }

    public <T> T getAttribute(@NonNls String name) {
        return cast(attributes.get(name));
    }

    public DatabaseOperation withAttribute(@NonNls String name, Object value) {
        attributes.put(name, value);
        return this;
    }
}
