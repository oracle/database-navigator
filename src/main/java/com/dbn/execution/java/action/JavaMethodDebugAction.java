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

package com.dbn.execution.java.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Progress;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.execution.java.wrapper.JavaExecutionWrapperManager;
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.action.AnObjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class JavaMethodDebugAction extends AnObjectAction<DBJavaMethod> {

    private final boolean listElement;

    public JavaMethodDebugAction(DBJavaMethod method, boolean listElement) {
        super(method);
        this.listElement = listElement;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBJavaMethod method) {
        String methodSignature = method.getPresentableText();

        Progress.prompt(project, null, true, "Debugging java method", "Creating execution wrappers for java method \"" + methodSignature + "\"", progress -> {
            try {
                JavaExecutionWrapperManager wrapperManager = JavaExecutionWrapperManager.getInstance(getProject());
                Wrapper wrapper = wrapperManager.createExecutionWrappers(method, false, true);

                String sqlWrapperName = wrapper.getSqlWrapperName();
                DBMethod dbMethod = method.getSchema().getMethod(sqlWrapperName, (short) 0);
                DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
                editorManager.connectAndOpenEditor(dbMethod, null, false, true);

                DatabaseDebuggerManager executionManager = DatabaseDebuggerManager.getInstance(project);
                executionManager.startJavaDebugger(method);
            } catch (Exception ex) {
                showErrorDialog(project, "Error creating debug wrappers for java method \"" + methodSignature + "\"\nCause: " + ex.getMessage());
                conditionallyLog(ex);
            }
        });
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBJavaMethod target) {
        if (listElement) {
            super.update(e, presentation, project, target);
        } else {
            presentation.setText(txt("app.execution.action.Debug"));
            presentation.setIcon(Icons.METHOD_EXECUTION_DEBUG);
        }
    }
}
