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

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;

/**
 * A thin horizontal progress bar used to visualize percentage-based metrics.
 */
public class MLProgressBarPanel extends JPanel {

    private final int percentage;
    private final int barHeight;

    public MLProgressBarPanel(int percentage, int barHeight) {
        this.percentage = Math.max(0, Math.min(100, percentage));
        this.barHeight = barHeight;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int y = (getHeight() - barHeight) / 2;
        g.setColor(JBColor.LIGHT_GRAY);
        g.fillRect(0, y, getWidth(), barHeight);
        g.setColor(JBColor.namedColor("ProgressBar.progressColor", new JBColor(new Color(75, 110, 175), new Color(90, 140, 210))));
        g.fillRect(0, y, getWidth() * percentage / 100, barHeight);
    }
}
