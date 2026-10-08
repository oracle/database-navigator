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

package com.dbn.migration.shared.workspace.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspace;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspaceBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the engine-specific details form displayed by the shared workspace form.
 *
 * @param <W> workspace type managed by the details form
 * @param <B> workspace bundle type providing the details form state
 */
@FunctionalInterface
public interface DatabaseMigrationWorkspaceFormFactory<
        W extends DatabaseMigrationWorkspace,
        B extends DatabaseMigrationWorkspaceBundle<W>> {

    @NotNull
    DBNForm create(
            @NotNull DBNComponent parent,
            @NotNull B workspaces,
            @NotNull W workspace);
}
