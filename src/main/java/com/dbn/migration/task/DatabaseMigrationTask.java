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

package com.dbn.migration.task;

import com.dbn.common.util.Named;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Presentational contract for an item displayed by a database migration dashboard.
 *
 * <p>The contract is shared by operations and workflows. It deliberately contains only
 * dashboard metadata so that each migration engine can keep its execution model and
 * engine-specific capabilities separate.</p>
 */
public interface DatabaseMigrationTask extends Named {
    @NotNull
    String getDashboardName();

    @NotNull
    String getDashboardDescription();

    @Nullable
    default String getDashboardDocumentationUrl() {
        return null;
    }
}
