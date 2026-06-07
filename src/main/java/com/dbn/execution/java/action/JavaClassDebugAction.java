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
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.ui.JavaExecutionHistory;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.action.ObjectListShowAction;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.nls.NlsResources.txt;

public class JavaClassDebugAction extends ObjectListShowAction {

    public JavaClassDebugAction(DBJavaClass javaClass) {
        super(txt("app.execution.action.Debug"), javaClass);
        getTemplatePresentation().setIcon(Icons.METHOD_EXECUTION_DEBUG);
    }

    @Nullable
    @Override
    public List<DBObject> getRecentObjectList() {
        DBJavaClass dbJavaClass = (DBJavaClass) getSourceObject();
        JavaExecutionManager javaExecutionManager = JavaExecutionManager.getInstance(dbJavaClass.getProject());
        JavaExecutionHistory executionHistory = javaExecutionManager.getExecutionHistory();
        return cast(executionHistory.getRecentlyExecutedMethods(dbJavaClass));
    }


    @Override
    public List<DBObject> getObjectList() {
        DBJavaClass dbJavaClass = (DBJavaClass) getSourceObject();
        return new ArrayList<>(dbJavaClass.getStaticMethods());
    }

    @Override
    public String getTitle() {
        return txt("app.execution.action.SelectMethodToDebug");
    }

    @Override
    public String getEmptyListMessage() {
        DBJavaClass program = (DBJavaClass) getSourceObject();
        return txt("app.execution.action.NoMethods", program.getQualifiedNameWithType());
    }

    @Override
    public String getListName() {
        return txt("app.execution.token.DebuggableElements");
    }

    @Override
    protected AnAction createObjectAction(DBObject object) {
        return new JavaMethodDebugAction((DBJavaMethod) object, true);
    }
}
