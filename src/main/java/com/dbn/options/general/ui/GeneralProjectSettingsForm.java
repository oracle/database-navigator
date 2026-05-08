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

package com.dbn.options.general.ui;

import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.options.general.GeneralProjectSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.util.FileChoosers.nativeFileChoosers;

public class GeneralProjectSettingsForm extends CompositeConfigurationEditorForm<GeneralProjectSettings> {
    private JPanel mainPanel;
    private JPanel localeSettingsPanel;
    private JPanel environmentSettingsPanel;
    private JPanel environmentOptionsPanel;
    private JCheckBox nativeFileChoosersCheckBox;

    public GeneralProjectSettingsForm(GeneralProjectSettings generalSettings) {
        super(generalSettings);
        resetFormChanges();

        registerComponent(mainPanel);

        localeSettingsPanel.add(generalSettings.getRegionalSettings().createComponent(), BorderLayout.CENTER);
        environmentSettingsPanel.add(generalSettings.getEnvironmentSettings().createComponent(), BorderLayout.CENTER);
    }

    @Override
    public void resetFormChanges() {
        super.resetFormChanges();
        nativeFileChoosersCheckBox.setSelected(nativeFileChoosers);
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        nativeFileChoosers = nativeFileChoosersCheckBox.isSelected();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
