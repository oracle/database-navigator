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

package com.dbn.code.common.completion.options.ui;

import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class CodeCompletionSettingsForm extends CompositeConfigurationEditorForm<CodeCompletionSettings> {
    private JPanel mainPanel;
    private JPanel filterPanel;
    private JPanel sortingPanel;
    private JPanel formatPanel;

    public CodeCompletionSettingsForm(CodeCompletionSettings codeCompletionSettings) {
        super(codeCompletionSettings);

        filterPanel.add(codeCompletionSettings.getFilterSettings().createComponent(), BorderLayout.CENTER);
        sortingPanel.add(codeCompletionSettings.getSortingSettings().createComponent(), BorderLayout.CENTER);
        formatPanel.add(codeCompletionSettings.getFormatSettings().createComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
