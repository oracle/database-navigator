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

package com.dbn.common.expression;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Slf4j
@State(
        name = ExpressionEvaluationManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ExpressionEvaluationManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.ExpressionEvaluationManager";
    private final GroovyExpressionEvaluator expressionEvaluator = new GroovyExpressionEvaluator();

    private ExpressionEvaluationManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ExpressionEvaluationManager getInstance(@NotNull Project project) {
        return Components.projectService(project, ExpressionEvaluationManager.class);
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull final Element element) {
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
    }
}