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

package com.dbn.execution;

import com.dbn.common.ui.window.DBNToolWindowFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.icon.Icons.WINDOW_EXECUTION_CONSOLE;

public class ExecutionConsoleToolWindowFactory extends DBNToolWindowFactory {
    @Override
    protected void initialize(@NotNull ToolWindow toolWindow) {
        toolWindow.setTitle("DB Execution Console");
        toolWindow.setStripeTitle("DB Execution Console");
        toolWindow.setIcon(WINDOW_EXECUTION_CONSOLE.get());

    }

    @Override
    public void createContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // no content by default - will be created on demand
        // (just set default visibility behavior)

        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setAutoHide(false);
        toolWindow.setAvailable(false, null);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return false;
    }
}
