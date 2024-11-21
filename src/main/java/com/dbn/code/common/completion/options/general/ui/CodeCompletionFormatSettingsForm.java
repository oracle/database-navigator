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

package com.dbn.code.common.completion.options.general.ui;

import com.dbn.code.common.completion.options.general.CodeCompletionFormatSettings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class CodeCompletionFormatSettingsForm extends ConfigurationEditorForm<CodeCompletionFormatSettings> {
    private JCheckBox enforceCaseCheckBox;
    private JPanel mainPanel;

    public CodeCompletionFormatSettingsForm(CodeCompletionFormatSettings settings) {
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
        CodeCompletionFormatSettings settings = getConfiguration();
        settings.setEnforceCodeStyleCase(enforceCaseCheckBox.isSelected());
    }

    @Override
    public void resetFormChanges() {
        CodeCompletionFormatSettings settings = getConfiguration();
        enforceCaseCheckBox.setSelected(settings.isEnforceCodeStyleCase());
    }
}
