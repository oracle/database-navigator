/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Presentational contract for an item displayed by a Liquibase dashboard. */
public interface LiquibaseDashboardItem {
    @NotNull
    String getDashboardName();

    @NotNull
    String getDashboardDescription();

    @Nullable
    default String getDashboardDocumentationUrl() {
        return null;
    }
}
