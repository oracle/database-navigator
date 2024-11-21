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

package com.dbn.common.ui.dialog;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.UserInterface;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class OptionsDialogForm<O extends Presentable> extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel optionLabel;
    private DBNComboBox<O> optionComboBox;
    private JPanel optionDescriptionPanel;

    public OptionsDialogForm(OptionsDialog<O> dialog) {
        super(dialog);

        DBNHintForm optionDescriptionForm = new DBNHintForm(this, null, null, true);
        optionDescriptionPanel.add(optionDescriptionForm.getComponent());

        optionLabel.setText(dialog.getOptionLabel());

        O selectedOption = dialog.getSelectedOption();
        optionComboBox.setValues(dialog.getOptions());
        optionComboBox.setSelectedValue(selectedOption);
        if (selectedOption != null) optionDescriptionForm.setHintContent(selectedOption.getInfo());

        optionComboBox.addListener((oldValue, newValue) -> {
            optionDescriptionForm.setHintContent(newValue == null ? null : newValue.getInfo());
            dialog.setSelectedOption(newValue);
            dialog.setActionsEnabled(newValue != null);
            UserInterface.repaint(mainPanel);
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return optionComboBox;
    }
}
