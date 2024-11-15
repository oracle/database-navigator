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

package com.dbn.common.outcome;

import com.dbn.common.project.ProjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Stub implementation for an {@link OutcomeHandler} that heeds the Project context
 * (holds a weak reference to the project to avoid memory leaks if not disposed properly)
 *
 * @author Dan Cioca (Oracle)
 */
public abstract class ProjectOutcomeHandler implements OutcomeHandler {
    private final ProjectRef project;

    public ProjectOutcomeHandler(Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }
}
