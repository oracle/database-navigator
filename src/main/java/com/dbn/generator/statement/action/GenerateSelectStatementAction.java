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
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GenerateSelectStatementAction extends GenerateStatementAction {
    private final List<DBObjectRef<DBObject>> selectedObjectRefs;

    GenerateSelectStatementAction(List<DBObject> selectedObjects) {
        this.selectedObjectRefs = DBObjectRef.from(selectedObjects);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        e.getPresentation().setText("SELECT Statement");
    }

    @Override
    protected StatementGeneratorResult generateStatement(Project project) {
        StatementGenerationManager statementGenerationManager = StatementGenerationManager.getInstance(project);
        List<DBObject> selectedObjects = getSelectedObjects();
        return statementGenerationManager.generateSelectStatement(selectedObjects, true);
    }

    private List<DBObject> getSelectedObjects() {
        return DBObjectRef.ensure(selectedObjectRefs);
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        List<DBObject> selectedObjects = getSelectedObjects();
        if (selectedObjects.size() > 0) {
            return selectedObjects.get(0).getConnection();
        }
        return null;
    }
}
