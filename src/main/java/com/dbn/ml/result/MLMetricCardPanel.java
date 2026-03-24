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

package com.dbn.ml.result;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * A card-style panel that displays a single ML metric.
 * Layout is declared in {@code MLMetricCardPanel.form}; data is set in the constructor.
 */
public class MLMetricCardPanel extends JPanel {

    private static final int BAR_HEIGHT = 4;

    // Form-bound fields
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel valueLabel;
    private JPanel progressBarContainer;

    public MLMetricCardPanel(String name, double value, boolean isRatio) {
        super(new BorderLayout());
        // $$$setupUI$$$() is injected here by IntelliJ's form compiler
        add(mainPanel);

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(0)));   // inner padding comes from the form margin

        nameLabel.setText(name);
        nameLabel.setFont(nameLabel.getFont().deriveFont(11f));
        nameLabel.setForeground(JBColor.gray);

        valueLabel.setText(isRatio ? String.format("%.1f%%", value * 100) : String.format("%.4f", value));
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 18f));

        if (isRatio) {
            progressBarContainer.add(new MLProgressBarPanel((int) (value * 100), BAR_HEIGHT), BorderLayout.CENTER);
        } else {
            progressBarContainer.setVisible(false);
        }
    }
}
