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

package com.dbn.execution.compiler.action;

import com.dbn.common.action.BasicAction;
import com.dbn.connection.operation.options.OperationSettings;
import com.dbn.editor.DBContentType;
import com.dbn.execution.compiler.CompileType;
import com.dbn.execution.compiler.CompilerAction;
import com.dbn.execution.compiler.CompilerActionSource;
import com.dbn.execution.compiler.DatabaseCompilerManager;
import com.dbn.execution.compiler.options.CompilerSettings;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.common.status.DBObjectStatus.COMPILING;
import static com.dbn.object.common.status.DBObjectStatus.DEBUG;
import static com.dbn.object.common.status.DBObjectStatus.PRESENT;
import static com.dbn.object.common.status.DBObjectStatus.VALID;

public class CompileObjectAction extends BasicAction {
    private final DBObjectRef<DBSchemaObject> objectRef;
    private final DBContentType contentType;
    private final CompileType compileType;

    public CompileObjectAction(DBSchemaObject object, DBContentType contentType, CompileType compileType) {
        super(txt("app.compiler.action.Compile"));
        this.objectRef = DBObjectRef.of(object);
        this.contentType = contentType;
        this.compileType = compileType;
    }

    public DBSchemaObject getObject() {
        return DBObjectRef.ensure(objectRef);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DBSchemaObject object = getObject();
        DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(object.getProject());
        CompilerAction compilerAction = new CompilerAction(CompilerActionSource.COMPILE, contentType);
        compilerManager.compileInBackground(object, compileType, compilerAction);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DBSchemaObject object = getObject();
        Presentation presentation = e.getPresentation();


        presentation.setEnabled(isEnabled());

        boolean debug = compileType == CompileType.DEBUG;
        String objectTypeName = object.getObjectType().getName();
        String text = debug ?
                txt("app.compiler.action.CompileObjectDebug", objectTypeName):
                txt("app.compiler.action.CompileObject", objectTypeName);

        presentation.setText(text);
    }

    private boolean isEnabled() {
        DBSchemaObject object = getObject();
        DBObjectStatusHolder status = object.getStatus();

        if (status.is(contentType, COMPILING)) return false;

        CompilerSettings settings = getCompilerSettings(object.getProject());
        if (settings.isAlwaysShowCompilerControls()) return true;


        boolean debug = compileType == CompileType.DEBUG;
        DBContentType[] subContentTypes = contentType.getSubContentTypes();
        if (subContentTypes.length > 0) {
            for (DBContentType subContentType : subContentTypes) {
                if (status.isNot(subContentType, PRESENT)) continue;
                if (status.isNot(subContentType, VALID)) return true;
                if (debug != status.is(subContentType, DEBUG)) return true;
            }

        } else {
            if (status.isNot(VALID)) return true;
            if (debug != status.is(DEBUG)) return true;

        }

        return false;
    }

    private static CompilerSettings getCompilerSettings(Project project) {
        return OperationSettings.getInstance(project).getCompilerSettings();
    }
}
