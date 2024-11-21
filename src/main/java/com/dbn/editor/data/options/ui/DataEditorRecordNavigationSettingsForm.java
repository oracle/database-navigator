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
import com.dbn.data.record.navigation.RecordNavigationTarget;
import com.dbn.editor.data.options.DataEditorRecordNavigationSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class DataEditorRecordNavigationSettingsForm extends ConfigurationEditorForm<DataEditorRecordNavigationSettings> {
    private JPanel mainPanel;
    private JComboBox<RecordNavigationTarget> navigationTargetComboBox;


    public DataEditorRecordNavigationSettingsForm(DataEditorRecordNavigationSettings configuration) {
        super(configuration);
        initComboBox(navigationTargetComboBox,
                RecordNavigationTarget.EDITOR,
                RecordNavigationTarget.VIEWER,
                RecordNavigationTarget.ASK);

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
        DataEditorRecordNavigationSettings configuration = getConfiguration();

        RecordNavigationTarget navigationTarget = getSelection(navigationTargetComboBox);
        configuration.setNavigationTarget(navigationTarget);
    }

    @Override
    public void resetFormChanges() {
        DataEditorRecordNavigationSettings configuration = getConfiguration();
        RecordNavigationTarget navigationTarget = configuration.getNavigationTarget();
        setSelection(navigationTargetComboBox, navigationTarget);
    }
}
