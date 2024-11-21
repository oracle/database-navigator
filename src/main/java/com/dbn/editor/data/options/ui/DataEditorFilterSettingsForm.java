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
import com.dbn.editor.data.filter.DatasetFilterType;
import com.dbn.editor.data.options.DataEditorFilterSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.event.ActionListener;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class DataEditorFilterSettingsForm extends ConfigurationEditorForm<DataEditorFilterSettings> {
    private JPanel mainPanel;
    private JCheckBox promptFilterDialogCheckBox;
    private JComboBox<DatasetFilterType> defaultFilterTypeComboBox;

    public DataEditorFilterSettingsForm(DataEditorFilterSettings settings) {
        super(settings);
        initComboBox(defaultFilterTypeComboBox,
                DatasetFilterType.NONE,
                DatasetFilterType.BASIC,
                DatasetFilterType.CUSTOM);

        resetFormChanges();
        defaultFilterTypeComboBox.setEnabled(promptFilterDialogCheckBox.isSelected());
        registerComponent(mainPanel);
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            getConfiguration().setModified(true);
            defaultFilterTypeComboBox.setEnabled(promptFilterDialogCheckBox.isSelected());
        };
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DataEditorFilterSettings settings = getConfiguration();
        settings.setPromptFilterDialog(promptFilterDialogCheckBox.isSelected());
        settings.setDefaultFilterType(getSelection(defaultFilterTypeComboBox));
    }


    @Override
    public void resetFormChanges() {
        DataEditorFilterSettings settings = getConfiguration();
        promptFilterDialogCheckBox.setSelected(settings.isPromptFilterDialog());
        setSelection(defaultFilterTypeComboBox, settings.getDefaultFilterType());
    }
}
