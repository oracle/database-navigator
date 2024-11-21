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
import com.dbn.browser.options.BrowserDisplayMode;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ToggleAction;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class BrowserDisplayModeAction extends ToggleAction {
    private final BrowserDisplayMode displayMode;

    protected BrowserDisplayModeAction(BrowserDisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    private static DatabaseBrowserManager getBrowserManager(@NotNull AnActionEvent e) {
        Project project = Lookups.ensureProject(e);

        return DatabaseBrowserManager.getInstance(project);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        DatabaseBrowserManager browserManager = getBrowserManager(e);
        BrowserDisplayMode displayMode = browserManager.getSettings().getGeneralSettings().getDisplayMode();
        return this.displayMode == displayMode;

    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        if (!state) return;

        DatabaseBrowserManager browserManager = getBrowserManager(e);
        browserManager.changeDisplayMode(displayMode);
        IdeEventQueue.getInstance().getPopupManager().closeAllPopups();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Presentation presentation = e.getPresentation();
        presentation.setText(this.displayMode.getName());
    }

    public static class Simple extends BrowserDisplayModeAction {
        protected Simple() {
            super(BrowserDisplayMode.SIMPLE);
        }
    }

    public static class Tabbed extends BrowserDisplayModeAction {
        protected Tabbed() {
            super(BrowserDisplayMode.TABBED);
        }
    }

    public static class Selector extends BrowserDisplayModeAction {
        protected Selector() {
            super(BrowserDisplayMode.SELECTOR);
        }
    }
}
