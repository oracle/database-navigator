/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.ml.prediction.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Tooltips;
import com.dbn.ml.prediction.MLPredictionAttribute;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;

public class MLPredictionAttributeForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel attributeLabel;
    private JTextField valueTextField;
    private JLabel dataTypeLabel;

    MLPredictionAttributeForm(@NotNull MLPredictionForm parent, @NotNull MLPredictionAttribute attribute) {
        super(parent);
        attributeLabel.setIcon(Icons.DBO_COLUMN);
        attributeLabel.setText(attribute.getName());
        dataTypeLabel.setText(attribute.getDisplayDataType());
        dataTypeLabel.setForeground(UIUtil.getInactiveTextColor());
        valueTextField.setPreferredSize(new JBDimension(200, -1));
        Tooltips.setToolTipText(valueTextField, attribute.getDisplayDataType());
    }

    @Override
    protected void initAccessibility() {
        setAccessibleUnit(valueTextField, dataTypeLabel.getText());
    }

    @Override
    protected void initFieldAlignment() {
        getFieldAlignerData().registerFieldGroup(attributeLabel, dataTypeLabel);
    }

    public JTextField getValueTextField() {
        return valueTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
