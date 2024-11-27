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

package com.dbn.debugger.jdbc.config;

import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.ExecutionConfigManager;
import com.dbn.debugger.common.config.DBMethodRunConfig;
import com.dbn.debugger.common.config.DBMethodRunConfigFactory;
import com.dbn.debugger.common.config.DBMethodRunConfigType;
import com.dbn.debugger.common.config.DBRunConfigCategory;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.object.DBMethod;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class DBMethodJdbcRunConfigFactory extends DBMethodRunConfigFactory<DBMethodRunConfigType, DBMethodRunConfig> {
    public DBMethodJdbcRunConfigFactory(@NotNull DBMethodRunConfigType type) {
        super(type, DBDebuggerType.JDBC);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DBMethodRunConfig(project, this, "", DBRunConfigCategory.TEMPLATE);
    }

    @Override
    public DBMethodRunConfig createConfiguration(Project project, String name, DBRunConfigCategory category) {
        return new DBMethodRunConfig(project, this, name, category);
    }

    @Override
    public DBMethodRunConfig createConfiguration(DBMethod method) {
        Project project = method.getProject();
        ExecutionConfigManager executionConfigManager = ExecutionConfigManager.getInstance(project);
        String name = executionConfigManager.createMethodConfigurationName(method);

        DBMethodRunConfig runConfiguration = new DBMethodRunConfig(project, this, name, DBRunConfigCategory.CUSTOM);
        MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
        MethodExecutionInput executionInput = executionManager.getExecutionInput(method);
        runConfiguration.setExecutionInput(executionInput);
        return runConfiguration;
    }
}
