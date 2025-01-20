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
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.editor.data.options.DataEditorQualifiedEditorSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.options.ui.ConfigurationEditors.validateIntegerValue;

public class DataEditorQualifiedEditorSettingsForm extends ConfigurationEditorForm<DataEditorQualifiedEditorSettings> {
    private JPanel mainPanel;
    private JTextField textLengthThresholdTextField;
    private CheckBoxList checkBoxList;

    public DataEditorQualifiedEditorSettingsForm(DataEditorQualifiedEditorSettings settings) {
        super(settings);
        checkBoxList.setElements(settings.getContentTypes());
        resetFormChanges();
        registerComponent(mainPanel);
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DataEditorQualifiedEditorSettings settings = getConfiguration();
        checkBoxList.applyChanges();
        settings.setTextLengthThreshold(validateIntegerValue(
                textLengthThresholdTextField,
                "Text Length Threshold", true, 0, 999999999, null));
    }

    @Override
    public void resetFormChanges() {
        DataEditorQualifiedEditorSettings settings = getConfiguration();
        textLengthThresholdTextField.setText(Integer.toString(settings.getTextLengthThreshold()));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
