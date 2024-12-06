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
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isValid;

public abstract class ContextAction<T> extends ProjectAction {

    public ContextAction() {}

    public ContextAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    protected abstract T getContext(@NotNull AnActionEvent e);

    @Override
    protected final void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        T context = getContext(e);
        if (isValid(context)) {
            actionPerformed(e, project, context);
        }
    }

    @Override
    protected final void update(@NotNull AnActionEvent e, @NotNull Project project) {
        T target = getContext(e);
        boolean enabled = isValid(target);

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        update(e, presentation, project, target);
    }

    protected abstract void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull T target);

    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable T target){};

}
