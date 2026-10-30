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

package com.dbn.options.general;

import com.dbn.options.ProjectSettings;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/**
 * DBN feature groups, including optional features and core features that are always enabled.
 */
@Getter
public enum WorkspaceFeature {
    CONNECTION_MANAGEMENT(true,
            txt("cfg.workspace.label.WorkspaceFeature_CONNECTION_MANAGEMENT"),
            txt("cfg.workspace.text.WorkspaceFeature_CONNECTION_MANAGEMENT")),
    DATABASE_BROWSER(true,
            txt("cfg.workspace.label.WorkspaceFeature_DATABASE_BROWSER"),
            txt("cfg.workspace.text.WorkspaceFeature_DATABASE_BROWSER")),
    CODE_EDITOR(true,
            txt("cfg.workspace.label.WorkspaceFeature_CODE_EDITOR"),
            txt("cfg.workspace.text.WorkspaceFeature_CODE_EDITOR")),
    DATA_EDITOR(true,
            txt("cfg.workspace.label.WorkspaceFeature_DATA_EDITOR"),
            txt("cfg.workspace.text.WorkspaceFeature_DATA_EDITOR")),
    EXECUTION_ENGINE(true,
            txt("cfg.workspace.label.WorkspaceFeature_EXECUTION_ENGINE"),
            txt("cfg.workspace.text.WorkspaceFeature_EXECUTION_ENGINE")),
    TRANSACTIONS(true,
            txt("cfg.workspace.label.WorkspaceFeature_TRANSACTIONS"),
            txt("cfg.workspace.text.WorkspaceFeature_TRANSACTIONS")),
    SESSION_BROWSER(true,
            txt("cfg.workspace.label.WorkspaceFeature_SESSION_BROWSER"),
            txt("cfg.workspace.text.WorkspaceFeature_SESSION_BROWSER")),
    DATA_EXPORT(true,
            txt("cfg.workspace.label.WorkspaceFeature_DATA_EXPORT"),
            txt("cfg.workspace.text.WorkspaceFeature_DATA_EXPORT")),
    DDL_INTEGRATION(true,
            txt("cfg.workspace.label.WorkspaceFeature_DDL_INTEGRATION"),
            txt("cfg.workspace.text.WorkspaceFeature_DDL_INTEGRATION")),
    DEBUGGER(false,
            txt("cfg.workspace.label.WorkspaceFeature_DEBUGGER"),
            txt("cfg.workspace.text.WorkspaceFeature_DEBUGGER")),
    DATABASE_ASSISTANT(false,
            txt("cfg.workspace.label.WorkspaceFeature_DATABASE_ASSISTANT"),
            txt("cfg.workspace.text.WorkspaceFeature_DATABASE_ASSISTANT")),
    VECTOR_TOOLBOX(false,
            txt("cfg.workspace.label.WorkspaceFeature_VECTOR_TOOLBOX"),
            txt("cfg.workspace.text.WorkspaceFeature_VECTOR_TOOLBOX")),
    MCP_SERVER_BUILDER(false,
            txt("cfg.workspace.label.WorkspaceFeature_MCP_SERVER_BUILDER"),
            txt("cfg.workspace.text.WorkspaceFeature_MCP_SERVER_BUILDER")),
    MACHINE_LEARNING(false,
            txt("cfg.workspace.label.WorkspaceFeature_MACHINE_LEARNING"),
            txt("cfg.workspace.text.WorkspaceFeature_MACHINE_LEARNING")),
    OJVM(false,
            txt("cfg.workspace.label.WorkspaceFeature_OJVM"),
            txt("cfg.workspace.text.WorkspaceFeature_OJVM")),
    LIQUIBASE(false,
            txt("cfg.workspace.label.WorkspaceFeature_LIQUIBASE"),
            txt("cfg.workspace.text.WorkspaceFeature_LIQUIBASE")),
    EVENT_MONITOR(false,
            txt("cfg.workspace.label.WorkspaceFeature_EVENT_MONITOR"),
            txt("cfg.workspace.text.WorkspaceFeature_EVENT_MONITOR")),
    RESOURCE_MONITOR(false,
            txt("cfg.workspace.label.WorkspaceFeature_RESOURCE_MONITOR"),
            txt("cfg.workspace.text.WorkspaceFeature_RESOURCE_MONITOR"));

    private final boolean alwaysOn;
    private final @Nls String displayName;
    private final @Nls String description;

    WorkspaceFeature(boolean alwaysOn, @Nls String displayName, @Nls String description) {
        this.alwaysOn = alwaysOn;
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isEnabled(@NotNull Project project) {
        return alwaysOn || ProjectSettings.get(project)
                .getWorkspaceSettings()
                .isEnabled(this);
    }
}
