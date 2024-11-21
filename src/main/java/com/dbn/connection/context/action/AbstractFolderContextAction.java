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

package com.dbn.connection.context.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFolderContextAction extends ProjectAction {

    protected static FileConnectionContext getFileContext(@Nullable VirtualFile file, @NotNull Project project) {
        if (file == null || !file.isDirectory()) return null;

        FileConnectionContextManager contextManager = getContextManager(project);
        FileConnectionContext mapping = contextManager.getMapping(file);
        if (mapping != null && mapping.isForFile(file)) {
            return mapping;
        }
        return null;
    }

    protected static FileConnectionContextManager getContextManager(@NotNull Project project) {
        return FileConnectionContextManager.getInstance(project);
    }
}
