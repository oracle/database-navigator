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

package com.dbn.liquibase.execution;

import com.dbn.object.type.DBObjectType;
import liquibase.structure.DatabaseObject;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.UNKNOWN;

/** Utilities for mapping Liquibase database objects to DBN object types. */
@UtilityClass
public final class LiquibaseDatabaseObjects {
    private static final Map<String, DBObjectType> TYPE_ALIASES = Map.of(
            "primarykey", CONSTRAINT,
            "foreignkey", CONSTRAINT,
            "uniqueconstraint", CONSTRAINT,
            "checkconstraint", CONSTRAINT,
            "notnullconstraint", CONSTRAINT);

    @NotNull
    public static DBObjectType resolveObjectType(@Nullable DatabaseObject databaseObject) {
        if (databaseObject == null) return UNKNOWN;

        String typeName = databaseObject.getObjectTypeName();
        if (typeName == null || typeName.isEmpty()) return UNKNOWN;

        String normalizedTypeName = typeName
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);

        DBObjectType objectType = TYPE_ALIASES.get(normalizedTypeName);
        if (objectType != null) return objectType;

        objectType = DBObjectType.get(typeName);
        if (objectType != null) return objectType;

        return UNKNOWN;
    }
}
