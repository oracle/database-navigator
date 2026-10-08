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
 * Optional DBN feature groups whose entry points can be hidden for a project.
 */
@Getter
public enum WorkspaceFeature {
    DEBUGGER(
            txt("cfg.workspace.label.WorkspaceFeature_DEBUGGER"),
            txt("cfg.workspace.text.WorkspaceFeature_DEBUGGER")),
    DATABASE_ASSISTANT(
            txt("cfg.workspace.label.WorkspaceFeature_DATABASE_ASSISTANT"),
            txt("cfg.workspace.text.WorkspaceFeature_DATABASE_ASSISTANT")),
    VECTOR_TOOLBOX(
            txt("cfg.workspace.label.WorkspaceFeature_VECTOR_TOOLBOX"),
            txt("cfg.workspace.text.WorkspaceFeature_VECTOR_TOOLBOX")),
    MCP_SERVER_BUILDER(
            txt("cfg.workspace.label.WorkspaceFeature_MCP_SERVER_BUILDER"),
            txt("cfg.workspace.text.WorkspaceFeature_MCP_SERVER_BUILDER")),
    MACHINE_LEARNING(
            txt("cfg.workspace.label.WorkspaceFeature_MACHINE_LEARNING"),
            txt("cfg.workspace.text.WorkspaceFeature_MACHINE_LEARNING")),
    OJVM(
            txt("cfg.workspace.label.WorkspaceFeature_OJVM"),
            txt("cfg.workspace.text.WorkspaceFeature_OJVM")),
    LIQUIBASE(
            txt("cfg.workspace.label.WorkspaceFeature_LIQUIBASE"),
            txt("cfg.workspace.text.WorkspaceFeature_LIQUIBASE")),
    FLYWAY(
            txt("cfg.workspace.label.WorkspaceFeature_FLYWAY"),
            txt("cfg.workspace.text.WorkspaceFeature_FLYWAY")),
    EVENT_MONITOR(
            txt("cfg.workspace.label.WorkspaceFeature_EVENT_MONITOR"),
            txt("cfg.workspace.text.WorkspaceFeature_EVENT_MONITOR")),
    RESOURCE_MONITOR(
            txt("cfg.workspace.label.WorkspaceFeature_RESOURCE_MONITOR"),
            txt("cfg.workspace.text.WorkspaceFeature_RESOURCE_MONITOR"));

    private final @Nls String displayName;
    private final @Nls String description;

    WorkspaceFeature(@Nls String displayName, @Nls String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isEnabled(@NotNull Project project) {
        return ProjectSettings.get(project)
                .getWorkspaceSettings()
                .isEnabled(this);
    }
}
