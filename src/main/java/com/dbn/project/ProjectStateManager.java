/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.project;

import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.guarded;

public class ProjectStateManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.StateManager";

    protected ProjectStateManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ProjectStateManager getInstance(@NotNull Project project) {
        return Components.projectService(project, ProjectStateManager.class);
    }

    public static void registerDisposable(Project project, Disposable child) {
        guarded(() -> {
            if (isNotValid(project)) return;
            ProjectStateManager stateManager = ProjectStateManager.getInstance(project);
            Disposer.register(stateManager, child);
        });
    }
}
