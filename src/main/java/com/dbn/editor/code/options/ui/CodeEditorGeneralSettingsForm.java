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

package com.dbn.editor.code.options.ui;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dbn.language.common.SpellcheckingSettingsListener;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class CodeEditorGeneralSettingsForm extends ConfigurationEditorForm<CodeEditorGeneralSettings> {
    private JCheckBox showObjectNavigationGutterCheckBox;
    private JCheckBox specDeclarationGutterCheckBox;
    private JPanel mainPanel;
    private JCheckBox enableSpellchecking;
    private JCheckBox enableReferenceSpellchecking;

    public CodeEditorGeneralSettingsForm(CodeEditorGeneralSettings settings) {
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
        CodeEditorGeneralSettings configuration = getConfiguration();
        configuration.setShowObjectsNavigationGutter(showObjectNavigationGutterCheckBox.isSelected());
        configuration.setShowSpecDeclarationNavigationGutter(specDeclarationGutterCheckBox.isSelected());

        boolean enableSpellchecking = this.enableSpellchecking.isSelected();
        boolean enableReferenceSpellchecking = this.enableReferenceSpellchecking.isSelected();
        boolean spellcheckingSettingsChanged =
                configuration.isEnableSpellchecking() != enableSpellchecking ||
                configuration.isEnableReferenceSpellchecking() != enableReferenceSpellchecking;

        configuration.setEnableSpellchecking(enableSpellchecking);
        configuration.setEnableReferenceSpellchecking(enableReferenceSpellchecking);

        Project project = configuration.getProject();
        if (spellcheckingSettingsChanged) {
            SettingsChangeNotifier.register(
                    () -> ProjectEvents.notify(project,
                            SpellcheckingSettingsListener.TOPIC,
                            (listener) -> listener.settingsChanged()));
        }
    }

    @Override
    public void resetFormChanges() {
        CodeEditorGeneralSettings settings = getConfiguration();
        showObjectNavigationGutterCheckBox.setSelected(settings.isShowObjectsNavigationGutter());
        specDeclarationGutterCheckBox.setSelected(settings.isShowSpecDeclarationNavigationGutter());
        enableSpellchecking.setSelected(settings.isEnableSpellchecking());
        enableReferenceSpellchecking.setSelected(settings.isEnableReferenceSpellchecking());
    }
}
