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

package com.dbn.browser.action;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ToggleAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class AutoscrollToEditorAction extends ToggleAction implements DumbAware{

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        Project project = Lookups.getProject(e);
        if (project != null) {
            DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
            return browserManager.getAutoscrollToEditor().value();
        }
        return false;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        Project project = Lookups.getProject(e);
        if (project != null) {
            DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
            browserManager.getAutoscrollToEditor().setValue(state);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setText("Autoscroll to Editor");
    }
}
