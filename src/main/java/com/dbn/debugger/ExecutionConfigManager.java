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

package com.dbn.debugger;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.text.TextContent;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Naming;
import com.dbn.debugger.common.config.DBMethodRunConfig;
import com.dbn.debugger.common.config.DBMethodRunConfigFactory;
import com.dbn.debugger.common.config.DBMethodRunConfigType;
import com.dbn.debugger.common.config.DBRunConfig;
import com.dbn.debugger.common.config.DBStatementRunConfig;
import com.dbn.debugger.common.config.DBStatementRunConfigFactory;
import com.dbn.debugger.common.config.DBStatementRunConfigType;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.object.DBMethod;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.text.TextContent.plain;
import static com.dbn.debugger.ExecutionConfigManager.COMPONENT_NAME;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ExecutionConfigManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ExecutionConfigManager";


    public static final TextContent GENERIC_METHOD_RUNNER_HINT =
            plain("This is the generic Database Method debug runner. " +
                    "This is used when debugging is invoked on a given method. " +
                    "No specific method information can be specified here.");

    public static final TextContent GENERIC_STATEMENT_RUNNER_HINT =
            plain("This is the generic Database Statement debug runner. " +
                    "This is used when debugging is invoked on a given SQL statement. " +
                    "No specific statement information can be specified here.");


    private ExecutionConfigManager(Project project) {
        super(project, COMPONENT_NAME);
    }


    public static ExecutionConfigManager getInstance(@NotNull Project project) {
        return projectService(project, ExecutionConfigManager.class);
    }

    public DBMethodRunConfigType getMethodConfigurationType() {
        List<ConfigurationType> configurationTypes = ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList();
        return ContainerUtil.findInstance(configurationTypes, DBMethodRunConfigType.class);
    }

    public DBStatementRunConfigType getStatementConfigurationType() {
        List<ConfigurationType> configurationTypes = ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList();
        return ContainerUtil.findInstance(configurationTypes, DBStatementRunConfigType.class);
    }

    public String createMethodConfigurationName(DBMethod method) {
        DBMethodRunConfigType configurationType = getMethodConfigurationType();
        List<RunnerAndConfigurationSettings> configurationSettings = getRunManager().getConfigurationSettingsList(configurationType);

        String name = method.getName();
        while (nameExists(configurationSettings, name)) {
            name = Naming.nextNumberedIdentifier(name, true);
        }
        return name;
    }

    private static boolean nameExists(List<RunnerAndConfigurationSettings> configurationSettings, String name) {
        return Lists.anyMatch(configurationSettings, configurationSetting -> Objects.equals(configurationSetting.getName(), name));
    }

    @NotNull
    public RunnerAndConfigurationSettings createConfiguration(@NotNull DBMethod method, DBDebuggerType debuggerType) {
        DBMethodRunConfigType configType = getMethodConfigurationType();
        DBMethodRunConfigFactory<?, ?> configFactory = configType.getConfigurationFactory(debuggerType);
        DBMethodRunConfig config = configFactory.createConfiguration(method);

        return getRunManager().createConfiguration(config, configFactory);
    }

    public RunnerAndConfigurationSettings createConfiguration(@NotNull StatementExecutionProcessor executionProcessor, DBDebuggerType debuggerType) {
        DBStatementRunConfigType configType = getStatementConfigurationType();
        DBStatementRunConfigFactory<?, ?> configFactory = configType.getConfigurationFactory(debuggerType);
        DBStatementRunConfig config = configFactory.createConfiguration(executionProcessor);

        return getRunManager().createConfiguration(config, configFactory);
    }

    @NotNull
    private RunManager getRunManager() {
        return RunManagerEx.getInstance(ensureProject());
    }

    @Deprecated // TODO move to stateless run configuration (decommission after a few releases)
    public void removeRunConfigurations() {
        RunManager runManager = getRunManager();
        List<RunnerAndConfigurationSettings> runConfigurations = runManager.getAllSettings();
        for (RunnerAndConfigurationSettings runConfiguration : runConfigurations) {
            RunConfiguration configuration = runConfiguration.getConfiguration();
            if (configuration instanceof DBRunConfig) {
                runManager.removeConfiguration(runConfiguration);
            }
        }
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {

    }
}
