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

package com.dbn.migration.engine;

import com.dbn.common.extension.ExtensionPointCache;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides access to migration engine descriptors registered by DBN plugins.
 */
public final class DatabaseMigrationEngineDescriptors
        extends ExtensionPointCache<String, DatabaseMigrationEngineDescriptor<?, ?>> {
    private static final DatabaseMigrationEngineDescriptors INSTANCE =
            new DatabaseMigrationEngineDescriptors();

    private DatabaseMigrationEngineDescriptors() {
        super(DatabaseMigrationEngineDescriptor.EP, DatabaseMigrationEngineDescriptor::getId);
    }

    @NotNull
    public static DatabaseMigrationEngineDescriptor<?, ?> get(@NotNull String id) {
        return INSTANCE.find(id);
    }

    @NotNull
    public static List<DatabaseMigrationEngineDescriptor<?, ?>> list() {
        return INSTANCE.all();
    }
}
