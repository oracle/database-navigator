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
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.operation.DatabaseOperation.EXECUTE_JAVA_CODE;
import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public class ProgramMethodExecuteAction extends ProgramMethodLaunchAction {

    @Override
    protected AnAction createExecutionAction(DBObject method) {
        if (method instanceof DBMethod) {
            DBMethod programMethod = (DBMethod) method;
            return new ExecuteMethodAction(programMethod);
        }

        if (method instanceof DBJavaMethod) {
            DBJavaMethod javaMethod = (DBJavaMethod) method;
            return new ExecuteJavaMethodAction(javaMethod);
        }
        return null;
    }

    @Override
    public void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean visible = isVisible(e);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(visible);
        presentation.setText(txt("app.codeEditor.action.ExecuteMethod"));
        presentation.setIcon(Icons.METHOD_EXECUTION_RUN);
    }

    private static class ExecuteMethodAction extends AnObjectAction<DBMethod> {
        ExecuteMethodAction(DBMethod method) {
            super(method);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBMethod object) {

            MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
            executionManager.startMethodExecution(object, DBDebuggerType.NONE);
        }
    }

    private static class ExecuteJavaMethodAction extends AnObjectAction<DBJavaMethod> {
        ExecuteJavaMethodAction(DBJavaMethod method) {
            super(method);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBJavaMethod object) {
            EXECUTE_JAVA_CODE.start(object, () -> startMethodExecution(object));
        }

        private static void startMethodExecution(@NotNull DBJavaMethod object) {
            Project project = object.getProject();
            JavaExecutionManager executionManager = JavaExecutionManager.getInstance(project);
            executionManager.startMethodExecution(object, DBDebuggerType.NONE);
        }
    }
}
