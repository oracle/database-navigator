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
import com.dbn.execution.compiler.CompileType;
import com.dbn.execution.compiler.DatabaseCompilerManager;
import com.dbn.execution.compiler.options.CompilerSettings;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CompileInvalidObjectsAction extends BasicAction {
    private DBObjectRef<DBSchema> schemaRef;
    public CompileInvalidObjectsAction(DBSchema schema) {
        super("Compile invalid objects");
        this.schemaRef = DBObjectRef.of(schema);
    }

    @NotNull
    public DBSchema getSchema() {
        return DBObjectRef.ensure(schemaRef);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DBSchema schema = getSchema();
        Project project = schema.getProject();
        DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
        compilerManager.compileInvalidObjects(schema, getCompilerSettings(project).getCompileType());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DBSchema schema = getSchema();
        CompileType compileType = getCompilerSettings(schema.getProject()).getCompileType();
        String text = "Compile Invalid Objects";
        if (compileType == CompileType.DEBUG) text = text + " (Debug)";
        if (compileType == CompileType.ASK) text = text + "...";

        e.getPresentation().setText(text);
    }

    private static CompilerSettings getCompilerSettings(Project project) {
        return OperationSettings.getInstance(project).getCompilerSettings();
    }
}