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

package com.dbn.execution.script.options;

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.project.ProjectSupplier;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.execution.common.options.ExecutionTimeoutSettings;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.CmdLineInterfaceBundle;
import com.dbn.execution.script.options.ui.ScriptExecutionSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ScriptExecutionSettings extends BasicProjectConfiguration<ExecutionEngineSettings, ScriptExecutionSettingsForm> implements ExecutionTimeoutSettings, ProjectSupplier {
    private CmdLineInterfaceBundle commandLineInterfaces = new CmdLineInterfaceBundle();
    private int executionTimeout = 300;

    public ScriptExecutionSettings(ExecutionEngineSettings parent) {
        super(parent);
    }

    @NotNull
    @Override
    public ScriptExecutionSettingsForm createConfigurationEditor() {
        return new ScriptExecutionSettingsForm(this);
    }

    @NotNull
    public CmdLineInterface getCommandLineInterface(String id) {
        return commandLineInterfaces.getInterface(id);
    }

    @Override
    public int getDebugExecutionTimeout() {
        return 0;
    }

    @Override
    public void setDebugExecutionTimeout(int timeout) {}


    @Override
    public String getConfigElementName() {
        return "script-execution";
    }

    @Override
    public void readConfiguration(Element element) {
        Element executorsElement = element.getChild("command-line-interfaces");
        commandLineInterfaces.readConfiguration(executorsElement);
        executionTimeout = Settings.getInteger(element, "execution-timeout", executionTimeout);
    }

    @Override
    public void writeConfiguration(Element element) {
        Element executorsElement = newElement(element, "command-line-interfaces");
        commandLineInterfaces.writeConfiguration(executorsElement);
        Settings.setInteger(element, "execution-timeout", executionTimeout);
    }
}
