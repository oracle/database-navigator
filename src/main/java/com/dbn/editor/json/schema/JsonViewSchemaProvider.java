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

package com.dbn.editor.json.schema;

import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonViewSchemaProvider implements JsonSchemaFileProvider {
    private final Map<String, VirtualFile> schemaFiles = new ConcurrentHashMap<>();

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
        return false;
    }

    @Override
    public @NotNull @Nls String getName() {
        return "Json View Schema";
    }

    @Override
    public @Nullable VirtualFile getSchemaFile() {
        return null;
    }

    @Override
    public @NotNull SchemaType getSchemaType() {
        return SchemaType.userSchema;
    }
}
