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

package com.dbn.editor.code.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.icon.Icons;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public class ProgramMethodDebugAction extends ProgramMethodLaunchAction {

    @Override
    protected AnAction createExecutionAction(DBObject method) {
        if (method instanceof DBMethod programMethod) {
            return new DebugMethodAction(programMethod);
        }

        if (method instanceof DBJavaMethod javaMethod) {
            return new DebugJavaMethodAction(javaMethod);
        }
        return null;
    }

    @Override
    public void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean visible = isVisible(e);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(visible);
        presentation.setText(txt("app.codeEditor.action.DebugMethod"));
        presentation.setIcon(Icons.METHOD_EXECUTION_DEBUG);
    }

    @Override
    protected boolean isVisible(@NotNull AnActionEvent e) {
        if (!super.isVisible(e)) return false;

        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        return DatabaseFeature.DEBUGGING.isSupported(sourceCodeFile);
    }

    private static class DebugMethodAction extends AnObjectAction<DBMethod> {
        DebugMethodAction(DBMethod method) {
            super(method);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBMethod method) {

            DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
            debuggerManager.startMethodDebugger(method);
        }
    }

    private static class DebugJavaMethodAction extends AnObjectAction<DBJavaMethod> {
        DebugJavaMethodAction(DBJavaMethod method) {
            super(method);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBJavaMethod method) {

            DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
            debuggerManager.startJavaDebugger(method);
        }
    }
}
