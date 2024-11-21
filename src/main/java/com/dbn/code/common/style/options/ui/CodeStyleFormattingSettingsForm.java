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

package com.dbn.code.common.style.options.ui;

import com.dbn.code.common.style.options.CodeStyleFormattingOption;
import com.dbn.code.common.style.options.CodeStyleFormattingSettings;
import com.dbn.code.common.style.presets.CodeStylePreset;
import com.dbn.common.Pair;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class CodeStyleFormattingSettingsForm extends ConfigurationEditorForm<CodeStyleFormattingSettings> {
    private JPanel mainPanel;
    private JPanel settingsPanel;
    private JCheckBox enableCheckBox;
    private final List<Pair<CodeStyleFormattingOption, JComboBox<CodeStylePreset>>> mappings = new ArrayList<>();

    public CodeStyleFormattingSettingsForm(CodeStyleFormattingSettings settings) {
        super(settings);
        CodeStyleFormattingOption[] options = settings.getOptions();
        settingsPanel.setLayout(new GridLayoutManager(options.length + 1, 2, JBUI.insets(4), -1, -1));
        for (int i=0; i< options.length; i++) {
            CodeStyleFormattingOption option = options[i];
            JLabel label = new JLabel(option.getDisplayName());
            settingsPanel.add(label,
                    new GridConstraints(i, 0, 1, 1,
                            GridConstraints.ANCHOR_WEST,
                            GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_FIXED,
                            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

            JComboBox<CodeStylePreset> comboBox = new ComboBox<>();
            initComboBox(comboBox, option.getPresets());
            label.setLabelFor(comboBox);

            settingsPanel.add(comboBox,
                    new GridConstraints(i, 1, 1, 1,
                            GridConstraints.ANCHOR_WEST,
                            GridConstraints.FILL_HORIZONTAL,
                            GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

            mappings.add(Pair.of(option, comboBox));
        }

        resetFormChanges();
        enableDisableOptions();
        
        settingsPanel.add(new Spacer(),
                new GridConstraints(options.length, 1, 1, 1,
                        GridConstraints.ANCHOR_CENTER,
                        GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));

        registerComponent(mainPanel);
        enableCheckBox.addActionListener(e -> enableDisableOptions());

        //Shortcut[] basicShortcuts = KeyUtil.getShortcuts("ReformatCode");
        //useOnReformatCheckBox.setText("Use on reformat code (" + KeymapUtil.getShortcutsText(basicShortcuts) + ")");
    }

    private void enableDisableOptions() {
        boolean selected = enableCheckBox.isSelected();
        for (Pair<CodeStyleFormattingOption, JComboBox<CodeStylePreset>> mapping : mappings) {
            mapping.second().setEnabled(selected);
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        for (val mapping : mappings) {
            CodeStyleFormattingOption option = mapping.first();
            JComboBox<CodeStylePreset> comboBox = mapping.second();
            option.setPreset(getSelection(comboBox));
        }
        getConfiguration().setEnabled(enableCheckBox.isSelected());
    }


    @Override
    public void resetFormChanges() {
        for (val mapping : mappings) {
            CodeStyleFormattingOption option = mapping.first();
            JComboBox<CodeStylePreset> comboBox = mapping.second();
            setSelection(comboBox, option.getPreset());
        }
        enableCheckBox.setSelected(getConfiguration().isEnabled());
    }
}
