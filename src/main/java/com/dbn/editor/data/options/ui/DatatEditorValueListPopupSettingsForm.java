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
import com.dbn.editor.data.options.DataEditorValueListPopupSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.options.ui.ConfigurationEditors.validateIntegerValue;

public class DatatEditorValueListPopupSettingsForm extends ConfigurationEditorForm<DataEditorValueListPopupSettings> {
    private JTextField elementCountThresholdTextBox;
    private JTextField dataLengthThresholdTextBox;
    private JCheckBox showPopupButtonCheckBox;
    private JPanel mainPanel;

    public DatatEditorValueListPopupSettingsForm(DataEditorValueListPopupSettings settings) {
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
        DataEditorValueListPopupSettings settings = getConfiguration();
        settings.setShowPopupButton(showPopupButtonCheckBox.isSelected());
        settings.setElementCountThreshold(validateIntegerValue(elementCountThresholdTextBox, "Element count threshold", true, 0, 10000, null));
        settings.setDataLengthThreshold(validateIntegerValue(dataLengthThresholdTextBox, "Data length threshold", true, 0, 1000, null));
    }

    @Override
    public void resetFormChanges() {
        DataEditorValueListPopupSettings settings = getConfiguration();
        showPopupButtonCheckBox.setSelected(settings.isShowPopupButton());
        elementCountThresholdTextBox.setText(Integer.toString(settings.getElementCountThreshold()));
        dataLengthThresholdTextBox.setText(Integer.toString(settings.getDataLengthThreshold()));
    }
}
