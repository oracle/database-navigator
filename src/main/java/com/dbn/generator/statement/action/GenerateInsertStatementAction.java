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

package com.dbn.generator.statement.action;

import com.dbn.connection.ConnectionHandler;
import com.dbn.generator.statement.StatementGenerationManager;
import com.dbn.generator.statement.StatementGeneratorResult;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class GenerateInsertStatementAction extends GenerateStatementAction {
    private DBObjectRef<DBTable> table;

    GenerateInsertStatementAction(DBTable table) {
        this.table = DBObjectRef.of(table);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.codeGenerator.action.InsertStatement"));
    }

    @Override
    protected StatementGeneratorResult generateStatement(Project project) {
        StatementGenerationManager statementGenerationManager = StatementGenerationManager.getInstance(project);
        DBTable table = getTable();
        return statementGenerationManager.generateInsert(table);
    }

    @NotNull
    private DBTable getTable() {
        return DBObjectRef.ensure(table);
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        return getTable().getConnection();
    }
}
