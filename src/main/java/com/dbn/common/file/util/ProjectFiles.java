/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.file.util;

import com.dbn.common.thread.Read;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.vfs.VfsUtilCore.getRelativePath;

@UtilityClass
public class ProjectFiles {

    public static ProjectFileIndex getProjectFileIndex(Project project) {
        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        return rootManager.getFileIndex();
    }

    @Nullable
    public static VirtualFile getProjectSourceRoot(Project project, VirtualFile file) {
        ProjectFileIndex index = getProjectFileIndex(project);
        return Read.call(index, i -> i.getSourceRootForFile(file));
    }

    public static boolean isProjectSourceFile(Project project, VirtualFile file) {
        return getProjectSourceRoot(project, file) != null;
    }

    public static String getProjectRelativePath(Project project, VirtualFile file) {
        VirtualFile sourceRoot = getProjectSourceRoot(project, file);
        if (sourceRoot == null) return file.getPath();

        return getRelativePath(file, sourceRoot);
    }
}
