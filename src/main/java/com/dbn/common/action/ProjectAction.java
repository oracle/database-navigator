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

package com.dbn.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions.ActionDescription;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.guarded;

public abstract class ProjectAction extends BasicAction {

    public ProjectAction() {}

    public ProjectAction(@Nullable @ActionText String text, @Nullable @ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public final void update(@NotNull AnActionEvent e) {
        guarded(this, a -> {
            Project project = a.resolveProject(e);
            if (isValid(project)) a.update(e, project);
        });
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e) {
        guarded(this, a -> {
            Project project = a.resolveProject(e);
            if (isValid(project)) a.actionPerformed(e, project);
        });
    }

    @Nullable
    private Project resolveProject(@NotNull AnActionEvent e) {
        Project project = getProject();
        if (project == null) project = Lookups.getProject(e);
        return project;
    }

    /**
     * fallback when project cannot be loaded from the data context (TODO check why)
     */
    @Nullable
    public Project getProject() {
        return null;
    }

    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    }

    protected abstract void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project);
}

