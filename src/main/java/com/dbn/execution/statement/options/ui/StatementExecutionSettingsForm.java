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

package com.dbn.execution.statement.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.execution.statement.options.StatementExecutionSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;

public class StatementExecutionSettingsForm extends ConfigurationEditorForm<StatementExecutionSettings> {
    private JPanel mainPanel;
    private JTextField fetchBlockSizeTextField;
    private JTextField executionTimeoutTextField;
    private JCheckBox focusResultCheckBox;
    private JTextField debugExecutionTimeoutTextField;
    private JCheckBox promptExecutionCheckBox;

    public StatementExecutionSettingsForm(StatementExecutionSettings settings) {
        super(settings);
        resetFormChanges();
        registerComponent(mainPanel);
    }

    @Override
    protected void initAccessibility() {
        setAccessibleUnit(fetchBlockSizeTextField, txt("app.shared.unit.Records"));
        setAccessibleUnit(executionTimeoutTextField, txt("app.shared.unit.Seconds"), txt("app.shared.hint.ZeroForNoTimeout"));
        setAccessibleUnit(debugExecutionTimeoutTextField, txt("app.shared.unit.Seconds"), txt("app.shared.hint.ZeroForNoTimeout"));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        StatementExecutionSettings configuration = getConfiguration();
        configuration.setResultSetFetchBlockSize(ConfigurationEditors.validateIntegerValue(fetchBlockSizeTextField, "Fetch block size", true, 1, 10000, null));
        int executionTimeout = ConfigurationEditors.validateIntegerValue(executionTimeoutTextField, "Execution timeout", true, 0, 6000, "\nUse value 0 for no timeout");
        int debugExecutionTimeout = ConfigurationEditors.validateIntegerValue(debugExecutionTimeoutTextField, "Debug execution timeout", true, 0, 6000, "\nUse value 0 for no timeout");

        configuration.setFocusResult(focusResultCheckBox.isSelected());
        configuration.setPromptExecution(promptExecutionCheckBox.isSelected());

        configuration.setExecutionTimeout(executionTimeout);
        configuration.setDebugExecutionTimeout(debugExecutionTimeout);
    }

    @Override
    public void resetFormChanges() {
        StatementExecutionSettings settings = getConfiguration();
        fetchBlockSizeTextField.setText(Integer.toString(settings.getResultSetFetchBlockSize()));
        executionTimeoutTextField.setText(Integer.toString(settings.getExecutionTimeout()));
        debugExecutionTimeoutTextField.setText(Integer.toString(settings.getDebugExecutionTimeout()));
        focusResultCheckBox.setSelected(settings.isFocusResult());
        promptExecutionCheckBox.setSelected(settings.isPromptExecution());
    }
}
