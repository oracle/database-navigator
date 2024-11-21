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

package com.dbn.browser;

import com.dbn.browser.ui.BrowserToolWindowForm;
import com.dbn.common.ui.window.DBNToolWindowFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.icon.Icons.WINDOW_DATABASE_BROWSER;

public class DatabaseBrowserToolWindowFactory extends DBNToolWindowFactory {

    @Override
    protected void initialize(@NotNull ToolWindow toolWindow) {
        toolWindow.setTitle("DB Browser");
        toolWindow.setStripeTitle("DB Browser");
        toolWindow.setIcon(WINDOW_DATABASE_BROWSER.get());
    }

    @Override
    public void createContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        BrowserToolWindowForm toolWindowForm = browserManager.getToolWindowForm();

        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = contentManager.getFactory();
        Content content = contentFactory.createContent(toolWindowForm.getComponent(), null, true);

        contentManager.addContent(content);
    }
}
