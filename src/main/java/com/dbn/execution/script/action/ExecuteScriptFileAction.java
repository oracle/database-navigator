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

package com.dbn.execution.script.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.execution.script.ScriptExecutionManager;
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public class ExecuteScriptFileAction extends ProjectAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (isAvailableFor(virtualFile)) {
            ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(project);
            scriptExecutionManager.executeScript(virtualFile);
        }
    }

    private boolean isAvailableFor(VirtualFile virtualFile) {
        return virtualFile != null && (
                virtualFile.getFileType() == SQLFileType.INSTANCE ||
                virtualFile.getFileType() == PSQLFileType.INSTANCE);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        boolean visible = isAvailableFor(virtualFile);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(visible);
        presentation.setText("Execute SQL Script");
        presentation.setIcon(Icons.EXECUTE_SQL_SCRIPT);
    }
}
