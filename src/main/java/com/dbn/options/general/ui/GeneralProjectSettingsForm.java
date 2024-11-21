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
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class GeneralProjectSettingsForm extends CompositeConfigurationEditorForm<GeneralProjectSettings> {
    private JPanel mainPanel;
    private JPanel localeSettingsPanel;
    private JPanel environmentSettingsPanel;

    public GeneralProjectSettingsForm(GeneralProjectSettings generalSettings) {
        super(generalSettings);
        resetFormChanges();

        registerComponent(mainPanel);

        localeSettingsPanel.add(generalSettings.getRegionalSettings().createComponent(), BorderLayout.CENTER);
        environmentSettingsPanel.add(generalSettings.getEnvironmentSettings().createComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
