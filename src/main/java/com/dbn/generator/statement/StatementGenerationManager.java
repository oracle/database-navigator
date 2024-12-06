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

package com.dbn.generator.statement;

import com.dbn.common.component.ProjectComponentBase;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.common.component.Components.projectService;

public class StatementGenerationManager extends ProjectComponentBase {

    public static final String COMPONENT_NAME = "DBNavigator.Project.StatementGenerationManager";

    private StatementGenerationManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static StatementGenerationManager getInstance(@NotNull Project project) {
        return projectService(project, StatementGenerationManager.class);
    }

    public StatementGeneratorResult generateSelectStatement(List<DBObject> objects, boolean enforceAliasUsage) {
        SelectStatementGenerator generator = new SelectStatementGenerator(objects, enforceAliasUsage);
        return generator.generateStatement(getProject());
    }

    public StatementGeneratorResult generateInsert(DBTable table) {
        InsertStatementGenerator generator = new InsertStatementGenerator(table);
        return generator.generateStatement(getProject());
    }
}
