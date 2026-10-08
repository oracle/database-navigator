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

package com.dbn.migration.shared.workspace;

import com.dbn.common.index.Identifiable;
import com.dbn.common.util.Named;
import com.dbn.common.util.UUIDs;
import com.dbn.connection.DatabaseType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Base state shared by database migration workspaces.
 *
 * <p>The base stores workspace identity, display name, target database type, and
 * configured root path. Concrete migration engines remain responsible for their
 * file layout and for resolving the root path in the surrounding project.</p>
 */
@Getter
@Setter
public abstract class DatabaseMigrationWorkspace implements Named, Identifiable<String> {
    private String id = UUIDs.regular();
    private String name;
    private DatabaseType databaseType;
    private String rootPath;

    protected DatabaseMigrationWorkspace(@NotNull String rootPath) {
        this.rootPath = rootPath;
    }
}
