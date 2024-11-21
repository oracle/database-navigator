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

package com.dbn.execution.statement.options;

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.project.ProjectSupplier;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.execution.common.options.ExecutionTimeoutSettings;
import com.dbn.execution.statement.options.ui.StatementExecutionSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class StatementExecutionSettings extends BasicProjectConfiguration<ExecutionEngineSettings, ConfigurationEditorForm> implements ExecutionTimeoutSettings, ProjectSupplier {
    private static final String REMEMBER_OPTION_HINT = ""/*"\n\n(you can remember your option and change it at any time in Settings > Execution Engine > Statement Execution)"*/;

    private int resultSetFetchBlockSize = 100;
    private int executionTimeout = 20;
    private int debugExecutionTimeout = 600;
    private boolean focusResult = false;
    private boolean promptExecution = false;

    public StatementExecutionSettings(ExecutionEngineSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.execution.title.StatementExecution");
    }

    @Override
    public String getHelpTopic() {
        return "executionEngine";
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public ConfigurationEditorForm createConfigurationEditor() {
        return new StatementExecutionSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "statement-execution";
    }

    @Override
    public void readConfiguration(Element element) {
        resultSetFetchBlockSize = Settings.getInteger(element, "fetch-block-size", resultSetFetchBlockSize);
        executionTimeout = Settings.getInteger(element, "execution-timeout", executionTimeout);
        debugExecutionTimeout = Settings.getInteger(element, "debug-execution-timeout", debugExecutionTimeout);
        focusResult = Settings.getBoolean(element, "focus-result", focusResult);
        promptExecution = Settings.getBoolean(element, "prompt-execution", promptExecution);
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setInteger(element, "fetch-block-size", resultSetFetchBlockSize);
        Settings.setInteger(element, "execution-timeout", executionTimeout);
        Settings.setInteger(element, "debug-execution-timeout", debugExecutionTimeout);
        Settings.setBoolean(element, "focus-result", focusResult);
        Settings.setBoolean(element, "prompt-execution", promptExecution);
    }
}
