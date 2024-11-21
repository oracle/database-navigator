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

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.debugger.common.config.ui.DBStatementRunConfigEditor;
import com.dbn.debugger.jdbc.state.DBJdbcStatementRunProfileState;
import com.dbn.debugger.jdwp.state.DBJdwpStatementRunProfileState;
import com.dbn.debugger.options.DebuggerTypeOption;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.object.DBMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;

@Getter
@Setter
public class DBStatementRunConfig extends DBRunConfig<StatementExecutionInput> {
    private StatementExecutionInput executionInput;

    public DBStatementRunConfig(Project project, DBStatementRunConfigFactory factory, String name, DBRunConfigCategory category) {
        super(project, factory, name, category);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DBStatementRunConfigEditor(this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        DBDebuggerType debuggerType = getDebuggerType();
        return debuggerType == DBDebuggerType.JDBC ? new DBJdbcStatementRunProfileState(env) :
               debuggerType == DBDebuggerType.JDWP ? new DBJdwpStatementRunProfileState(env) : null;
    }

    @Override
    public boolean canRun() {
        if (!super.canRun()) return false;
        if (executionInput == null) return false;

        ConnectionHandler connection = executionInput.getConnection();
        if (isNotValid(connection)) return false;
        if (!DatabaseFeature.DEBUGGING.isSupported(connection)) return false;

        DebuggerTypeOption debuggerTypeOption = connection.getSettings().getDebuggerSettings().getDebuggerType().getSelectedOption();
        if (debuggerTypeOption == DebuggerTypeOption.JDWP && !DBDebuggerType.JDWP.isSupported()) return false;

        return true;
    }

    public void checkConfiguration() throws RuntimeConfigurationException {
        if (getCategory() != DBRunConfigCategory.CUSTOM) return;

        StatementExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) return;

        ConnectionHandler connection = executionInput.getConnection();
        if (connection == null) return;

        if (!DatabaseFeature.DEBUGGING.isSupported(connection)){
            throw new RuntimeConfigurationError(
                    "Debugging is not supported for " + connection.getDatabaseType().getName() +" databases.");
        }

        DebuggerTypeOption debuggerTypeOption = connection.getSettings().getDebuggerSettings().getDebuggerType().getSelectedOption();
        if (debuggerTypeOption == DebuggerTypeOption.JDWP) {
            DatabaseDebuggerManager.checkJdwpConfiguration();
        }
    }

    @Nullable
    @Override
    public DBLanguagePsiFile getDatabaseContext() {
        if (executionInput == null) return null;
        return executionInput.getExecutionProcessor().getPsiFile();
    }

    @Override
    public List<DBMethod> getMethods() {
        if (executionInput == null) return Collections.emptyList();

        ExecutablePsiElement executablePsiElement = executionInput.getExecutionProcessor().getCachedExecutable();
        if (executablePsiElement == null) return Collections.emptyList();

        List<DBMethod> methods = new ArrayList<>();
        executablePsiElement.collectObjectReferences(DBObjectType.METHOD, object -> {
            if (object instanceof DBMethod) {
                DBMethod method = (DBMethod) object;
                DBSchema schema = method.getSchema();
                if (!schema.isSystemSchema() && !schema.isPublicSchema()) {
                    methods.add(method);
                }
            }
        });
        return methods;
    }

    @Override
    @Nullable
    public String suggestedName() {
        if (getCategory() != DBRunConfigCategory.GENERIC) return null;

        String defaultRunnerName = getType().getDefaultRunnerName();
        if (getDebuggerType() == DBDebuggerType.JDWP) {
            defaultRunnerName = defaultRunnerName + " (JDWP)";
        }
        return defaultRunnerName;
    }

    @Override
    public @Nullable Icon getIcon() {
        Icon defaultIcon = super.getIcon();
        if (getCategory() != DBRunConfigCategory.CUSTOM) return defaultIcon;

        StatementExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) return defaultIcon;

        StatementExecutionProcessor executionProcessor = executionInput.getExecutionProcessor();
        if (executionProcessor == null) return defaultIcon;

        return executionProcessor.getIcon();
    }
}
