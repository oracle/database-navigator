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

package com.dbn.execution.java.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.execution.java.options.JavaExecutionSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class JavaExecutionSettingsForm extends ConfigurationEditorForm<JavaExecutionSettings> {
    private JPanel mainPanel;
    private JTextField executionTimeoutTextField;
    private JTextField debugExecutionTimeoutTextField;
    private JTextField parameterHistorySizeTextField;

    public JavaExecutionSettingsForm(JavaExecutionSettings settings) {
        super(settings);
        resetFormChanges();
        registerComponent(mainPanel);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        JavaExecutionSettings configuration = getConfiguration();
        int executionTimeout = ConfigurationEditors.validateIntegerValue(executionTimeoutTextField, "Execution timeout", true, 0, 6000, "\nUse value 0 for no timeout");
        int debugExecutionTimeout = ConfigurationEditors.validateIntegerValue(debugExecutionTimeoutTextField, "Debug execution timeout", true, 0, 6000, "\nUse value 0 for no timeout");
        int parameterHistorySize = ConfigurationEditors.validateIntegerValue(parameterHistorySizeTextField, "Parameter history size", true, 0, 3000, null);
        configuration.setParameterHistorySize(parameterHistorySize);

        configuration.setExecutionTimeout(executionTimeout);
        configuration.setDebugExecutionTimeout(debugExecutionTimeout);
    }

    @Override
    public void resetFormChanges() {
        JavaExecutionSettings settings = getConfiguration();
        executionTimeoutTextField.setText(Integer.toString(settings.getExecutionTimeout()));
        debugExecutionTimeoutTextField.setText(Integer.toString(settings.getDebugExecutionTimeout()));
        parameterHistorySizeTextField.setText(Integer.toString(settings.getParameterHistorySize()));
    }
}
