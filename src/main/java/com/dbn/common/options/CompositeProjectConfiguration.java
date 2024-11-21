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

package com.dbn.common.options;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.common.project.ProjectRef;
import com.dbn.project.ProjectStateManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class CompositeProjectConfiguration<P extends ProjectConfiguration, E extends CompositeConfigurationEditorForm>
        extends CompositeConfiguration<P, E>
        implements ProjectConfiguration<P, E> {

    private ProjectRef project;

    public CompositeProjectConfiguration(P parent) {
        super(parent);
        Disposer.register(parent, this);
    }

    public CompositeProjectConfiguration(@NotNull Project project) {
        super(null);
        this.project = ProjectRef.of(project);
        ProjectStateManager.registerDisposable(project, this);
    }

    @NotNull
    @Override
    public Project getProject() {
        if (project != null) {
            return project.ensure();
        }

        return getParent().ensureProject();
    }
}
