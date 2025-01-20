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

package com.dbn.editor.data.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.editor.data.options.DataEditorGeneralSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;

public class DataEditorGeneralSettingsForm extends ConfigurationEditorForm<DataEditorGeneralSettings> {
    private JPanel mainPanel;
    private JTextField fetchBlockSizeTextField;
    private JTextField fetchTimeoutTextField;
    private JCheckBox trimWhitespacesCheckBox;
    private JCheckBox convertEmptyToNullCheckBox;
    private JCheckBox selectContentOnEditCheckBox;
    private JCheckBox largeValuePreviewActiveCheckBox;

    public DataEditorGeneralSettingsForm(DataEditorGeneralSettings settings) {
        super(settings);
        resetFormChanges();

        registerComponent(mainPanel);
    }

    @Override
    protected void initAccessibility() {
        setAccessibleUnit(fetchBlockSizeTextField, txt("app.shared.unit.Records"));
        setAccessibleUnit(fetchTimeoutTextField, txt("app.shared.unit.Seconds"), txt("app.shared.hint.ZeroForNoTimeout"));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConfigurationEditors.validateIntegerValue(fetchBlockSizeTextField, "Fetch block size", true, 1, 10000, null);
        ConfigurationEditors.validateIntegerValue(fetchTimeoutTextField, "Fetch timeout", true, 0, 300, "\nUse value 0 for no timeout");

        DataEditorGeneralSettings settings = getConfiguration();
        settings.getFetchBlockSize().to(fetchBlockSizeTextField);
        settings.getFetchTimeout().to(fetchTimeoutTextField);
        settings.getTrimWhitespaces().to(trimWhitespacesCheckBox);
        settings.getConvertEmptyStringsToNull().to(convertEmptyToNullCheckBox);
        settings.getSelectContentOnCellEdit().to(selectContentOnEditCheckBox);
        settings.getLargeValuePreviewActive().to(largeValuePreviewActiveCheckBox);
    }

    @Override
    public void resetFormChanges() {
        DataEditorGeneralSettings settings = getConfiguration();
        settings.getFetchBlockSize().from(fetchBlockSizeTextField);
        settings.getFetchTimeout().from(fetchTimeoutTextField);
        settings.getTrimWhitespaces().from(trimWhitespacesCheckBox);
        settings.getConvertEmptyStringsToNull().from(convertEmptyToNullCheckBox);
        settings.getSelectContentOnCellEdit().from(selectContentOnEditCheckBox);
        settings.getLargeValuePreviewActive().from(largeValuePreviewActiveCheckBox);
    }
}
