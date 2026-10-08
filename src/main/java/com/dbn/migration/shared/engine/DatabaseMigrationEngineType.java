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

package com.dbn.migration.shared.engine;

import com.dbn.common.icon.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

/**
 * Identifies a database migration engine used by shared migration UI.
 *
 * <p>Each engine supplies the resource keys and presentation icon needed by
 * engine-neutral workspace screens. Additional engines can add their metadata
 * without changing the shared form or dialog.</p>
 */
public enum DatabaseMigrationEngineType {
    LIQUIBASE(Icons.DB_LIQUIBASE),
    FLYWAY(Icons.DB_FLYWAY);

    private final Icon icon;

    DatabaseMigrationEngineType(@NotNull Icon icon) {
        this.icon = icon;
    }

    @NotNull
    public String getWorkspaceTitle() {
        return txt("msg.migration.title.WorkspaceSettings_" + name());
    }

    @NotNull
    public String getNoWorkspacesHint() {
        return txt("app.migration.hint.NoWorkspaces_" + name());
    }

    @NotNull
    public Icon getIcon() {
        return icon;
    }
}
