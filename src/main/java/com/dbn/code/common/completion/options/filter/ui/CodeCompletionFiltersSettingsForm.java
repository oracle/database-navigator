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

package com.dbn.code.common.completion.options.filter.ui;

import com.dbn.code.common.completion.options.filter.CodeCompletionFilterSettings;
import com.dbn.code.common.completion.options.filter.CodeCompletionFiltersSettings;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Keyboard;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class CodeCompletionFiltersSettingsForm extends CompositeConfigurationEditorForm<CodeCompletionFiltersSettings> {

    private JLabel basicCompletionLabel;
    private JLabel extendedCompletionLabel;
    private JPanel mainPanel;
    private JPanel basicFilterPanel;
    private JPanel extendedFilterPanel;

    public CodeCompletionFiltersSettingsForm(CodeCompletionFiltersSettings filtersSettings) {
        super(filtersSettings);
        CodeCompletionFilterSettings basicFilterSettings = filtersSettings.getBasicFilterSettings();
        CodeCompletionFilterSettings extendedFilterSettings = filtersSettings.getExtendedFilterSettings();

        JComponent basicFilterComponent = basicFilterSettings.createComponent();
        JComponent extendedFilterComponent = extendedFilterSettings.createComponent();

        basicFilterPanel.add(basicFilterComponent, BorderLayout.CENTER);
        extendedFilterPanel.add(extendedFilterComponent, BorderLayout.CENTER);

        Shortcut[] basicShortcuts = Keyboard.getShortcuts(IdeActions.ACTION_CODE_COMPLETION);
        Shortcut[] extendedShortcuts = Keyboard.getShortcuts(IdeActions.ACTION_SMART_TYPE_COMPLETION);

        String basicCompletionShortcut = " (" + KeymapUtil.getShortcutsText(basicShortcuts) + ")";
        String extendedCompletionShortcut = " (" + KeymapUtil.getShortcutsText(extendedShortcuts) + ")";

        basicCompletionLabel.setText("Basic" + basicCompletionShortcut);
        extendedCompletionLabel.setText("Extended" + extendedCompletionShortcut);

        Accessibility.setAccessibleName(basicCompletionLabel, "Basic Code Completion" + basicCompletionShortcut);
        Accessibility.setAccessibleName(extendedCompletionLabel, "Extended Code Completion" + extendedCompletionShortcut);

        basicCompletionLabel.setLabelFor(basicFilterSettings.getPreferredFocusedComponent());
        extendedCompletionLabel.setLabelFor(extendedFilterSettings.getPreferredFocusedComponent());

    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
