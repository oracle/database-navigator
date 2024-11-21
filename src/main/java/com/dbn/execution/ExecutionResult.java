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

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.common.result.ui.ExecutionResultForm;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public interface ExecutionResult<F extends ExecutionResultForm> extends StatefulDisposable, DataProvider {

    @Nullable
    F createForm();

    @Nullable
    default F getForm() {
        Project project = getProject();
        ExecutionManager executionManager = ExecutionManager.getInstance(project);
        return executionManager.getExecutionResultForm(this);
    }

    @NotNull
    String getName();

    default void setName(@NotNull String name, boolean sticky) {}

    default @Nullable Object getData(@NotNull String dataId) {
        return null;
    }

    Icon getIcon();

    @NotNull
    Project getProject();

    ConnectionId getConnectionId();

    @NotNull
    ConnectionHandler getConnection();

    DBLanguagePsiFile createPreviewFile();

    ExecutionResult<F> getPrevious();

    void setPrevious(ExecutionResult<F> previous);
}
