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

package com.dbn.data.grid.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.data.grid.options.DataGridSortingSettings;
import com.dbn.data.grid.options.NullSortingOption;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class DataGridSortingSettingsForm extends ConfigurationEditorForm<DataGridSortingSettings> {
    private JPanel mainPanel;
    private JCheckBox enableZoomingCheckBox;
    private JTextField maxSortingColumnsTextField;
    private JComboBox<NullSortingOption> nullsPositionComboBox;

    public DataGridSortingSettingsForm(DataGridSortingSettings settings) {
        super(settings);
        initComboBox(nullsPositionComboBox, NullSortingOption.values());

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
        DataGridSortingSettings settings = getConfiguration();
        settings.setNullsFirst(getSelection(nullsPositionComboBox) == NullSortingOption.FIRST);
        int maxSortingColumns = ConfigurationEditors.validateIntegerValue(maxSortingColumnsTextField, "Max sorting columns", true, 0, 100, "Use value 0 for unlimited number of sorting columns");
        settings.setMaxSortingColumns(maxSortingColumns);
    }

    @Override
    public void resetFormChanges() {
        DataGridSortingSettings settings = getConfiguration();
        setSelection(nullsPositionComboBox, settings.isNullsFirst() ? NullSortingOption.FIRST : NullSortingOption.LAST);
        maxSortingColumnsTextField.setText(Integer.toString(settings.getMaxSortingColumns()));
    }
}
