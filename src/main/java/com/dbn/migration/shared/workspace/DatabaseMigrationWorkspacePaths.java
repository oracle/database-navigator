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

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Typed path contract for an engine's migration workspace.
 *
 * <p>Concrete engines retain their own layout-specific path accessors while shared
 * code can safely identify the workspace type and use the common root operations.</p>
 */
public interface DatabaseMigrationWorkspacePaths<W extends DatabaseMigrationWorkspace> {
    @NotNull
    W getWorkspace();

    @NotNull
    Path getContentRootPath();

    @NotNull
    String getRelativePath(@NotNull Path path);
}
