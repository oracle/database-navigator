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

package com.dbn.debugger.common.config;

import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public abstract class DBStatementRunConfigFactory<T extends DBStatementRunConfigType, C extends DBStatementRunConfig> extends DBRunConfigFactory<T, C> {
    protected DBStatementRunConfigFactory(T type, DBDebuggerType debuggerType) {
        super(type, debuggerType);
    }

    @NotNull
    @Override
    public T getType() {
        return (T) super.getType();
    }


    @Override
    public Icon getIcon(@NotNull RunConfiguration configuration) {
        Icon defaultIcon = getIcon();

        C runConfiguration = (C) configuration;
        if (runConfiguration.getCategory() != DBRunConfigCategory.CUSTOM) return defaultIcon;

        StatementExecutionInput executionInput = runConfiguration.getExecutionInput();
        if (executionInput == null) return defaultIcon;

        ExecutablePsiElement executablePsiElement = executionInput.getExecutablePsiElement();
        if (executablePsiElement == null) return defaultIcon;

        DBLanguagePsiFile file = executablePsiElement.getFile();

        return file.getIcon();
    }


    @Override
    public abstract C createConfiguration(Project project, String name, DBRunConfigCategory category);


    public abstract C createConfiguration(@NotNull StatementExecutionProcessor executionProcessor);
}
