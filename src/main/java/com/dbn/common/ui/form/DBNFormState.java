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

package com.dbn.common.ui.form;

import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.Presentable;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.ComboBoxes.initSelectionListener;
import static com.dbn.common.ui.util.ComboBoxes.selectElement;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Commons.nvl;

@UtilityClass
public class DBNFormState {

    public static <T extends Presentable> void initPersistence(JComboBox<T> comboBox, StateAttributes stateAttributes, @NonNls String stateAttribute) {
        initSelectionListener(comboBox, s -> stateAttributes.setAttribute(stateAttribute, s == null ? null : s.getName()));

        comboBox.addPropertyChangeListener(e -> {
            if ("model".equals(e.getPropertyName())) {
                selectElement(comboBox, stateAttributes.getAttribute(stateAttribute));
                if (comboBox.getSelectedItem() == null && comboBox.getItemCount() > 0) {
                    comboBox.setSelectedIndex(0);
                }
            }
        });

        String attribute = stateAttributes.getAttribute(stateAttribute);
        selectElement(comboBox, attribute);
    }

    public static void initPersistence(JTextField textField, StateAttributes stateAttributes, @NonNls String stateAttribute) {
        String attribute = stateAttributes.getAttribute(stateAttribute);

        textField.setText(nvl(attribute, ""));
        onTextChange(textField, e -> stateAttributes.setAttribute(stateAttribute, textField.getText().trim()));
    }
}
