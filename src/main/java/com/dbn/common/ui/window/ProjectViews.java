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

package com.dbn.common.ui.window;

import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utilities for selecting local paths in IntelliJ's Project View.
 */
@UtilityClass
public class ProjectViews {

    /**
     * Opens the Project tool window and selects the supplied local path.
     *
     * <p>The path may identify either a file or a directory. Missing paths are ignored.</p>
     */
    public static void selectPath(@NotNull Project project, @NotNull Path path) {
        if (!Files.exists(path)) return;

        Background.run(() -> {
            VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
            if (file == null) return;

            Dispatch.run(() -> {
                ToolWindow projectToolWindow = ToolWindows.getToolWindow(project, ToolWindowId.PROJECT_VIEW);
                if (projectToolWindow == null) return;

                projectToolWindow.show(() -> ProjectView.getInstance(project).select(null, file, true));
            });
        });
    }
}
