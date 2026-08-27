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

import com.dbn.common.ui.Presentable;
import com.dbn.connection.DatabaseType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

/** Supported Liquibase changelog file formats. */
@Getter
public enum LiquibaseChangelogFormat implements Presentable {
    XML("xml"),
    YAML("yaml"),
    JSON("json"),
    SQL("sql");

    private final String extension;
    private final String name;

    LiquibaseChangelogFormat(@NotNull String extension) {
        this.extension = extension;
        this.name = txt("app.liquibase.const.ChangelogFormat_" + name());
    }

    @NotNull
    public static LiquibaseChangelogFormat fromFileName(String fileName) {
        if (fileName != null) {
            String lowerCase = fileName.toLowerCase(java.util.Locale.ROOT);
            for (LiquibaseChangelogFormat format : values()) {
                if (lowerCase.endsWith("." + format.extension)) return format;
            }
        }
        return YAML;
    }

    @NotNull
    public String getFileExtension(@Nullable DatabaseType databaseType) {
        if (this == SQL && databaseType != null && databaseType != DatabaseType.GENERIC && databaseType != DatabaseType.UNKNOWN) {
            return databaseType.name().toLowerCase(java.util.Locale.ROOT) + ".sql";
        }
        return extension;
    }
}
