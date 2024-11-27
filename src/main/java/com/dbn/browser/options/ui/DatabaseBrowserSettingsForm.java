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

package com.dbn.browser.options.ui;

import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;


public class DatabaseBrowserSettingsForm extends CompositeConfigurationEditorForm<DatabaseBrowserSettings> {
    private JPanel mainPanel;
    private JPanel generalSettingsPanel;
    private JPanel filterSettingsPanel;
    private JPanel sortingSettingsPanel;
    private JPanel editorSettingsPanel;

    public DatabaseBrowserSettingsForm(DatabaseBrowserSettings settings) {
        super(settings);
        generalSettingsPanel.add(settings.getGeneralSettings().createComponent(), BorderLayout.CENTER);
        filterSettingsPanel.add(settings.getFilterSettings().createComponent(), BorderLayout.CENTER);
        sortingSettingsPanel.add(settings.getSortingSettings().createComponent(), BorderLayout.CENTER);
        editorSettingsPanel.add(settings.getEditorSettings().createComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
