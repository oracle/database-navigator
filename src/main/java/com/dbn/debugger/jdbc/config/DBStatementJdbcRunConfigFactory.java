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

import com.dbn.common.icon.Icons;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.common.config.DBRunConfigCategory;
import com.dbn.debugger.common.config.DBStatementRunConfig;
import com.dbn.debugger.common.config.DBStatementRunConfigFactory;
import com.dbn.debugger.common.config.DBStatementRunConfigType;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.common.dispose.Failsafe.nd;

public class DBStatementJdbcRunConfigFactory extends DBStatementRunConfigFactory<DBStatementRunConfigType, DBStatementRunConfig> {
    public DBStatementJdbcRunConfigFactory(@NotNull DBStatementRunConfigType type) {
        super(type, DBDebuggerType.JDBC);
    }

    @Override
    public DBStatementRunConfig createConfiguration(Project project, String name, DBRunConfigCategory category) {
        return new DBStatementRunConfig(project, this, name, category);
    }

    @Override
    public Icon getIcon(@NotNull RunConfiguration configuration) {
        return Icons.EXEC_STATEMENT_CONFIG;
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DBStatementRunConfig(project, this, "", DBRunConfigCategory.TEMPLATE);
    }

    @Override
    public DBStatementRunConfig createConfiguration(@NotNull StatementExecutionProcessor executionProcessor) {
        Project project = executionProcessor.getProject();
        VirtualFile virtualFile = nd(executionProcessor.getVirtualFile());
        String runnerName = virtualFile.getName();

        DBStatementRunConfig configuration = createConfiguration(project, runnerName, DBRunConfigCategory.CUSTOM);
        configuration.setExecutionInput(executionProcessor.getExecutionInput());
        return configuration;
    }
}
