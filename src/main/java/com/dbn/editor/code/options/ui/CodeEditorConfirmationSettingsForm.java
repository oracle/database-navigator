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

package com.dbn.editor.code.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.editor.code.options.CodeEditorChangesOption;
import com.dbn.editor.code.options.CodeEditorConfirmationSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class CodeEditorConfirmationSettingsForm extends ConfigurationEditorForm<CodeEditorConfirmationSettings> {
    private JPanel mainPanel;
    private JCheckBox confirmSaveCheckBox;
    private JCheckBox confirmRevertCheckBox;
    private JComboBox<CodeEditorChangesOption> disconnectSessionComboBox;

    public CodeEditorConfirmationSettingsForm(CodeEditorConfirmationSettings settings) {
        super(settings);

        initComboBox(disconnectSessionComboBox,
                CodeEditorChangesOption.ASK,
                CodeEditorChangesOption.SAVE,
                CodeEditorChangesOption.DISCARD);

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
        CodeEditorConfirmationSettings settings = getConfiguration();
        settings.getSaveChanges().setConfirm(confirmSaveCheckBox.isSelected());
        settings.getRevertChanges().setConfirm(confirmRevertCheckBox.isSelected());
        settings.getExitOnChanges().set(getSelection(disconnectSessionComboBox));
    }

    @Override
    public void resetFormChanges() {
        CodeEditorConfirmationSettings settings = getConfiguration();
        confirmSaveCheckBox.setSelected(settings.getSaveChanges().isConfirm());
        confirmRevertCheckBox.setSelected(settings.getRevertChanges().isConfirm());
        setSelection(disconnectSessionComboBox, settings.getExitOnChanges().get());
    }
}
