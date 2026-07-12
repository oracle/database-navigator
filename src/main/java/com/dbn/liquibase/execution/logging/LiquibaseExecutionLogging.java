/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.execution.logging;

import liquibase.structure.DatabaseObject;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

/** Utilities for classifying Liquibase execution output. */
public final class LiquibaseExecutionLogging {
    private static final Set<String> SECONDARY_OBJECT_TYPES = Set.of(
            "column",
            "index",
            "primarykey",
            "foreignkey",
            "uniqueconstraint",
            "checkconstraint");

    private LiquibaseExecutionLogging() {}

    public static boolean isLoggableObject(@NotNull DatabaseObject object) {
        String objectType = object.getObjectTypeName();
        if (objectType == null) return true;

        String normalizedType = objectType
                .replace("_", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);
        return !SECONDARY_OBJECT_TYPES.contains(normalizedType);
    }
}
