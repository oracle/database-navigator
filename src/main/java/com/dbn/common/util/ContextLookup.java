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

package com.dbn.common.util;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

/**
 * Connection context lookup utilities
 */
@UtilityClass
public class ContextLookup {

    @Nullable
    public static ConnectionHandler getConnection(Project project, @Nullable FileEditor editor) {
        if (editor == null) return null;

        VirtualFile file = editor.getFile();
        return getConnection(project, file);
    }

    @Nullable
    private static ConnectionHandler getConnection(Project project, @Nullable VirtualFile file) {
        if (file == null) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        return contextManager.getConnection(file);
    }

    @Nullable
    public static ConnectionId getConnectionId(Project project, @Nullable FileEditor editor) {
        ConnectionHandler connection = getConnection(project, editor);
        return connection == null ? null : connection.getConnectionId();
    }

    @Nullable
    public static ConnectionId getConnectionId(Project project, @Nullable VirtualFile file) {
        if (file == null) return null;

        ConnectionHandler connection = getConnection(project, file);
        return connection == null ? null : connection.getConnectionId();
    }

}
