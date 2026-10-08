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

package com.dbn.migration.flyway.workspace;

import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspaceBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Project-level container for Flyway workspaces and schema selection preferences.
 */
public class FlywayWorkspaceBundle
        extends DatabaseMigrationWorkspaceBundle<FlywayWorkspace> {
    public FlywayWorkspaceBundle(@NotNull Project project) {
        super(project);
    }

    @NotNull
    @Override
    protected FlywayWorkspace createWorkspaceInstance() {
        return new FlywayWorkspace();
    }

    @Override
    public FlywayWorkspaceBundle clone() {
        return (FlywayWorkspaceBundle) super.clone();
    }
}
