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
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectPopupAction;
import com.dbn.common.icon.Icons;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.ui.JavaExecutionHistory;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.execution.method.ui.MethodExecutionHistory;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBJavaAccessibility;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Actions.SEPARATOR;

@BackgroundUpdate
public class ProgramMethodRunAction extends ProjectPopupAction {

    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        Project project = e.getProject();
        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        if (project != null && sourceCodeFile != null) {
            DBSchemaObject schemaObject = sourceCodeFile.getObject();
            if (schemaObject.getObjectType().matches(DBObjectType.PROGRAM)) {

                MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(project);
                MethodExecutionHistory executionHistory = methodExecutionManager.getExecutionHistory();
                List<DBMethod> recentMethods = executionHistory.getRecentlyExecutedMethods((DBProgram) schemaObject);

                if (recentMethods != null) {
                    for (DBMethod method : recentMethods) {
                        RunMethodAction action = new RunMethodAction(method);
                        actions.add(action);
                    }
                    actions.add(SEPARATOR);
                }

                List<? extends DBObject> objects = schemaObject.collectChildObjects(DBObjectType.METHOD);
                for (DBObject object : objects) {
                    if (recentMethods == null || !recentMethods.contains(object)) {
                        RunMethodAction action = new RunMethodAction((DBMethod) object);
                        actions.add(action);
                    }
                }
            } else if (schemaObject.getObjectType().matches(DBObjectType.JAVA_CLASS)) {

                JavaExecutionManager javaExecutionManager = JavaExecutionManager.getInstance(project);
                JavaExecutionHistory executionHistory = javaExecutionManager.getExecutionHistory();
                List<DBJavaMethod> recentMethods = executionHistory.getRecentlyExecutedMethods((DBJavaClass) schemaObject);

                if (recentMethods != null) {
                    for (DBJavaMethod method : recentMethods) {
                        RunJavaMethodAction action = new RunJavaMethodAction(method);
                        actions.add(action);
                    }
                    actions.add(SEPARATOR);
                }

                List<? extends DBObject> objects = schemaObject.collectChildObjects(DBObjectType.JAVA_METHOD);
                for (DBObject object : objects) {
                    DBJavaMethod method = (DBJavaMethod) object;
                    if(method.isStatic() && method.getAccessibility() == DBJavaAccessibility.PUBLIC) {
                        if (recentMethods == null || !recentMethods.contains(method)) {
                            RunJavaMethodAction action = new RunJavaMethodAction(method);
                            actions.add(action);
                        }
                    }
                }
            }
        }

        return actions.toArray(new AnAction[0]);
    }

    @Nullable
    private DBSourceCodeVirtualFile getSourcecodeFile(AnActionEvent e) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        return virtualFile instanceof DBSourceCodeVirtualFile ? (DBSourceCodeVirtualFile) virtualFile : null;
    }


    @Override
    public void update(@NotNull AnActionEvent e, Project project) {
        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        Presentation presentation = e.getPresentation();
        boolean visible = false;
        if (sourceCodeFile != null) {
            DBSchemaObject schemaObject = sourceCodeFile.getObject();
            if (schemaObject.getObjectType().matches(DBObjectType.PROGRAM)
                    || schemaObject.getObjectType().matches(DBObjectType.JAVA_CLASS)) {
                visible = true;
            }
        }

        presentation.setVisible(visible);
        presentation.setText("Run Method");
        presentation.setIcon(Icons.METHOD_EXECUTION_RUN);
    }

    public class RunMethodAction extends AnObjectAction<DBMethod> {
        RunMethodAction(DBMethod method) {
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

    public class RunJavaMethodAction extends AnObjectAction<DBJavaMethod> {
        RunJavaMethodAction(DBJavaMethod method) {
            super(method);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBJavaMethod object) {

            JavaExecutionManager executionManager = JavaExecutionManager.getInstance(project);
            executionManager.startMethodExecution(object, DBDebuggerType.NONE);
        }
    }
}
