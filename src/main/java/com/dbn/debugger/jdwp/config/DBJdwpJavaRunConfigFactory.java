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

package com.dbn.debugger.jdwp.config;

import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.ExecutionConfigManager;
import com.dbn.debugger.common.config.DBJavaRunConfig;
import com.dbn.debugger.common.config.DBJavaRunConfigFactory;
import com.dbn.debugger.common.config.DBJavaRunConfigType;
import com.dbn.debugger.common.config.DBRunConfigCategory;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.object.DBJavaMethod;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class DBJdwpJavaRunConfigFactory extends DBJavaRunConfigFactory<DBJavaRunConfigType, DBJavaRunConfig> {
    public DBJdwpJavaRunConfigFactory(@NotNull DBJavaRunConfigType type) {
        super(type, DBDebuggerType.JDWP);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DBJavaRunConfig(project, this, "", DBRunConfigCategory.TEMPLATE);
    }

    @Override
    public DBJavaRunConfig createConfiguration(Project project, String name, DBRunConfigCategory category) {
        return new DBJavaRunConfig(project, this, name, category);
    }

    @Override
    public DBJavaRunConfig createConfiguration(DBJavaMethod method) {
        Project project = method.getProject();
        ExecutionConfigManager executionConfigManager = ExecutionConfigManager.getInstance(project);
        String name = executionConfigManager.createJavaMethodConfigurationName(method);
        name = name + " (JDWP)";
        DBJavaRunConfig runConfiguration = new DBJavaRunConfig(project, this, name, DBRunConfigCategory.CUSTOM);
        JavaExecutionManager executionManager = JavaExecutionManager.getInstance(project);
        JavaExecutionInput executionInput = executionManager.getExecutionInput(method);
        runConfiguration.setExecutionInput(executionInput);
        return runConfiguration;
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName() + " (JDWP)";
    }
}
