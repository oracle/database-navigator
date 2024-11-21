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

import com.dbn.browser.options.BrowserDisplayMode;
import com.dbn.browser.options.DatabaseBrowserGeneralSettings;
import com.dbn.browser.options.listener.DisplayModeSettingsListener;
import com.dbn.browser.options.listener.ObjectDetailSettingsListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class DatabaseBrowserGeneralSettingsForm extends ConfigurationEditorForm<DatabaseBrowserGeneralSettings> {
    private JPanel mainPanel;
    private JTextField navigationHistorySizeTextField;
    private JCheckBox showObjectDetailsCheckBox;
    private JCheckBox stickyTreePathCheckBox;
    private JComboBox<BrowserDisplayMode> browserTypeComboBox;


    public DatabaseBrowserGeneralSettingsForm(DatabaseBrowserGeneralSettings configuration) {
        super(configuration);

        initComboBox(browserTypeComboBox,
                BrowserDisplayMode.SIMPLE,
                BrowserDisplayMode.TABBED,
                BrowserDisplayMode.SELECTOR);

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
        DatabaseBrowserGeneralSettings configuration = getConfiguration();
        ConfigurationEditors.validateIntegerValue(navigationHistorySizeTextField, "Navigation history size", true, 0, 1000, "");

        boolean repaintTree = configuration.isModified();
        
        BrowserDisplayMode displayMode = getSelection(browserTypeComboBox);
        boolean displayModeChanged = configuration.getDisplayMode() != displayMode;
        configuration.setDisplayMode(displayMode);

        configuration.getNavigationHistorySize().to(navigationHistorySizeTextField);
        configuration.getShowObjectDetails().to(showObjectDetailsCheckBox);
        configuration.getEnableStickyPaths().to(stickyTreePathCheckBox);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (displayModeChanged) {
                ProjectEvents.notify(project,
                        DisplayModeSettingsListener.TOPIC,
                        (listener) -> listener.displayModeChanged(displayMode));

            } else if (repaintTree) {
                ProjectEvents.notify(project,
                        ObjectDetailSettingsListener.TOPIC,
                        (listener) -> listener.displayDetailsChanged());
            }
        });
    }

    @Override
    public void resetFormChanges() {
        DatabaseBrowserGeneralSettings configuration = getConfiguration();
        setSelection(browserTypeComboBox, configuration.getDisplayMode());

        configuration.getNavigationHistorySize().from(navigationHistorySizeTextField);
        configuration.getShowObjectDetails().from(showObjectDetailsCheckBox);
        configuration.getEnableStickyPaths().from(stickyTreePathCheckBox);
    }

}
