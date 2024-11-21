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

import com.dbn.common.dispose.Disposer;
import com.dbn.debugger.common.config.ui.DBProgramRunConfigForm;
import com.dbn.execution.ExecutionInput;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

import static com.dbn.common.dispose.Checks.isValid;

@Getter
public abstract class DBRunConfigEditor<T extends DBRunConfig<?>, F extends DBProgramRunConfigForm<T>, I extends ExecutionInput> extends SettingsEditor<T> {
    private final T configuration;
    private F configurationEditorForm;

    public DBRunConfigEditor(T configuration) {
        this.configuration = configuration;
    }

    protected abstract F createConfigurationEditorForm();

    public F getConfigurationEditorForm(boolean create) {
        if (create && !isValid(configurationEditorForm)) {
            configurationEditorForm = createConfigurationEditorForm();
        }
        return configurationEditorForm;
    }

    @Override
    protected void disposeEditor() {
        configurationEditorForm = Disposer.replace(configurationEditorForm, null);
    }

    @Override
    protected void resetEditorFrom(@NotNull T configuration) {
        getConfigurationEditorForm(true).readConfiguration(configuration);
    }

    @Override
    protected void applyEditorTo(@NotNull T configuration) throws ConfigurationException {
        getConfigurationEditorForm(true).writeConfiguration(configuration);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        configurationEditorForm = getConfigurationEditorForm(true);
        return configurationEditorForm.getComponent();
    }

    public abstract void setExecutionInput(I executionInput);
}
