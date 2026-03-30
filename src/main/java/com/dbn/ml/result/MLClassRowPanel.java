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

import javax.swing.*;
import java.awt.*;

/**
 * A horizontal row showing per-class precision / recall / F1 with inline progress bars.
 * Outer structure is declared in {@code MLClassRowPanel.form}; metric bars are built programmatically.
 */
public class MLClassRowPanel extends JPanel {

    private static final int BAR_HEIGHT = 4;

    // Form-bound fields
    private JPanel mainPanel;
    private JPanel namePanel;
    private JLabel nameLabel;
    private JLabel supportLabel;
    private JPanel metricsPanel;

    public MLClassRowPanel(String className, double precision, double recall, double f1, int support) {
        super(new BorderLayout());
        // $$$setupUI$$$() is injected here by IntelliJ's form compiler
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        add(mainPanel);

        nameLabel.setText(className);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));

        supportLabel.setText("(n=" + support + ")");
        supportLabel.setForeground(JBColor.gray);
        supportLabel.setFont(supportLabel.getFont().deriveFont(10f));

        metricsPanel.setLayout(new GridLayout(1, 3, 20, 0));
        metricsPanel.add(metricBar("P", precision));
        metricsPanel.add(metricBar("R", recall));
        metricsPanel.add(metricBar("F1", f1));
    }

    private static JPanel metricBar(String label, double value) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(labelComp.getFont().deriveFont(11f));
        labelComp.setForeground(JBColor.gray);
        labelComp.setPreferredSize(new Dimension(20, 16));
        panel.add(labelComp, BorderLayout.WEST);

        panel.add(new MLProgressBarPanel((int) (value * 100), BAR_HEIGHT), BorderLayout.CENTER);

        JLabel valueLabel = new JLabel(String.format("%.0f%%", value * 100));
        valueLabel.setFont(valueLabel.getFont().deriveFont(11f));
        valueLabel.setPreferredSize(new Dimension(40, 16));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(valueLabel, BorderLayout.EAST);

        return panel;
    }
}
