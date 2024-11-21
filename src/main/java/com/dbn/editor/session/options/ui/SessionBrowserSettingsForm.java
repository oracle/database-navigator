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

package com.dbn.editor.session.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.editor.session.options.SessionBrowserSettings;
import com.dbn.editor.session.options.SessionInterruptionOption;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class SessionBrowserSettingsForm extends ConfigurationEditorForm<SessionBrowserSettings> {
    private JPanel mainPanel;
    private JComboBox<SessionInterruptionOption> disconnectSessionComboBox;
    private JComboBox<SessionInterruptionOption> killSessionComboBox;
    private JCheckBox reloadOnFilterChangeCheckBox;

    public SessionBrowserSettingsForm(SessionBrowserSettings settings) {
        super(settings);

        initComboBox(disconnectSessionComboBox,
                SessionInterruptionOption.ASK,
                SessionInterruptionOption.IMMEDIATE,
                SessionInterruptionOption.POST_TRANSACTION);

        initComboBox(killSessionComboBox,
                SessionInterruptionOption.ASK,
                SessionInterruptionOption.NORMAL,
                SessionInterruptionOption.IMMEDIATE);

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
        SessionBrowserSettings settings = getConfiguration();
        settings.getDisconnectSession().set(getSelection(disconnectSessionComboBox));
        settings.getKillSession().set(getSelection(killSessionComboBox));
        settings.setReloadOnFilterChange(reloadOnFilterChangeCheckBox.isSelected());
    }

    @Override
    public void resetFormChanges() {
        SessionBrowserSettings settings = getConfiguration();
        setSelection(disconnectSessionComboBox, settings.getDisconnectSession().get());
        setSelection(killSessionComboBox, settings.getKillSession().get());
        reloadOnFilterChangeCheckBox.setSelected(settings.isReloadOnFilterChange());
    }
}
