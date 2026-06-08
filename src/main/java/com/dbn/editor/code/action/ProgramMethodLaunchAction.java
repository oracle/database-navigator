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
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.ui.JavaExecutionHistory;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.execution.method.ui.MethodExecutionHistory;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Actions.LOADING_SURROGATE;
import static com.dbn.common.util.Actions.SEPARATOR;
import static com.dbn.common.util.Actions.toActionArray;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_METHOD;
import static com.dbn.object.type.DBObjectType.METHOD;
import static com.dbn.object.type.DBObjectType.PROGRAM;

@BackgroundUpdate
public abstract class ProgramMethodLaunchAction extends ProjectPopupAction {

    protected ProgramMethodLaunchAction(String text) {
        super(text);
    }

    @Override
    public final AnAction[] getChildren(AnActionEvent e) {
        Project project = e.getProject();
        if (isNotValid(project)) return AnAction.EMPTY_ARRAY;

        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        if (isNotValid(sourceCodeFile)) return AnAction.EMPTY_ARRAY;

        DBSchemaObject schemaObject = sourceCodeFile.getObject();
        if (schemaObject instanceof DBProgram program) {
            return createProgramMethodActions(program);

        } else if (schemaObject instanceof DBJavaClass javaClass) {
            return createJavaMethodActions(javaClass);
        }

        return AnAction.EMPTY_ARRAY;
    }

    @Nullable
    protected DBSourceCodeVirtualFile getSourcecodeFile(AnActionEvent e) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        return virtualFile instanceof DBSourceCodeVirtualFile ? (DBSourceCodeVirtualFile) virtualFile : null;
    }

    protected abstract AnAction createExecutionAction(DBObject method);

    protected boolean isVisible(@NotNull AnActionEvent e) {
        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        if (sourceCodeFile == null) return false;

        DBSchemaObject schemaObject = sourceCodeFile.getObject();
        DBObjectType objectType = schemaObject.getObjectType();
        return objectType.matches(PROGRAM) || objectType.matches(JAVA_CLASS);
    }

    private AnAction[] createProgramMethodActions(DBProgram program) {
        DBObjectList<?>[] objectLists = program.getChildObjectLists();
        for (DBObjectList<?> objectList : objectLists) {
            DBObjectType objectType = objectList.getObjectType();
            if (objectType.matches(METHOD) && !objectList.isLoaded()) {
                objectList.loadInBackground();
                return LOADING_SURROGATE;
            }
        }

        List<AnAction> actions = new ArrayList<>();
        Project project = program.getProject();
        MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(project);
        MethodExecutionHistory executionHistory = methodExecutionManager.getExecutionHistory();
        List<DBMethod> recentMethods = executionHistory.getRecentlyExecutedMethods(program);

        if (recentMethods != null) {
            for (DBMethod method : recentMethods) {
                actions.add(createExecutionAction(method));
            }
            actions.add(SEPARATOR);
        }

        List<DBMethod> methods = program.collectChildObjects(DBObjectType.METHOD);
        for (DBMethod object : methods) {
            if (recentMethods == null || !recentMethods.contains(object)) {
                actions.add(createExecutionAction(object));
            }
        }

        return toActionArray(actions);
    }

    private AnAction[] createJavaMethodActions(DBJavaClass javaClass) {
        DBObjectList<DBObject> methodsList = javaClass.getChildObjectList(JAVA_METHOD);
        if (methodsList != null && !methodsList.isLoaded()) {
            // initialize the load
            methodsList.loadInBackground();
            return LOADING_SURROGATE;
        }

        Project project = javaClass.getProject();
        JavaExecutionManager javaExecutionManager = JavaExecutionManager.getInstance(project);
        JavaExecutionHistory executionHistory = javaExecutionManager.getExecutionHistory();
        List<DBJavaMethod> recentMethods = executionHistory.getRecentlyExecutedMethods((DBJavaClass) javaClass);

        List<AnAction> actions = new ArrayList<>();
        Set<DBObjectRef<DBJavaMethod>> methodRefs = new HashSet<>();
        if (recentMethods != null) {
            for (DBJavaMethod method : recentMethods) {
                actions.add(createExecutionAction(method));
            }
            actions.add(SEPARATOR);
            methodRefs = new HashSet<>(DBObjectRef.from(recentMethods));
        }

        List<DBJavaMethod> methods = javaClass.getChildObjects(JAVA_METHOD);
        for (DBJavaMethod method : methods) {
            if (!method.isExecutable()) continue;
            if (methodRefs.contains(method.ref())) continue;

            actions.add(createExecutionAction(method));
        }
        return toActionArray(actions);
    }
}
