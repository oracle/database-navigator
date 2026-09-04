/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workspace;

import com.dbn.connection.DatabaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Strings.isEmpty;

/** Utilities for naming Liquibase changelog files. */
public final class LiquibaseChangelogFiles {
    private static final String DEFAULT_MASTER_CHANGELOG_NAME = "db.changelog-master";

    private LiquibaseChangelogFiles() {
    }

    @NotNull
    public static String getDefaultMasterChangelog(
            @NotNull LiquibaseChangelogFormat format,
            @Nullable DatabaseType databaseType) {
        return DEFAULT_MASTER_CHANGELOG_NAME + "." + format.getFileExtension(databaseType);
    }

    @NotNull
    public static String normalize(
            @NotNull String fileName,
            @NotNull LiquibaseChangelogFormat format,
            @Nullable DatabaseType databaseType) {
        if (isEmpty(fileName)) return fileName;

        String lowerCase = fileName.toLowerCase(java.util.Locale.ROOT);
        for (DatabaseType type : DatabaseType.values()) {
            String extension = "." + type.name().toLowerCase(java.util.Locale.ROOT) + ".sql";
            if (lowerCase.endsWith(extension)) {
                return fileName.substring(0, fileName.length() - extension.length()) + "." + format.getFileExtension(databaseType);
            }
        }
        for (LiquibaseChangelogFormat current : LiquibaseChangelogFormat.values()) {
            String extension = "." + current.getExtension();
            if (lowerCase.endsWith(extension)) {
                return fileName.substring(0, fileName.length() - extension.length()) + "." + format.getFileExtension(databaseType);
            }
        }
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
        return baseName + "." + format.getFileExtension(databaseType);
    }
}
