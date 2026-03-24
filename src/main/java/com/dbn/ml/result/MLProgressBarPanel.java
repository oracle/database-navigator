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
 * Reusable thin progress bar panel used throughout ML result displays.
 */
public class MLProgressBarPanel extends JPanel {

    private final int percentage;

    public MLProgressBarPanel(int percentage, int barHeight) {
        this.percentage = percentage;
        setPreferredSize(new Dimension(60, barHeight));
        setMinimumSize(new Dimension(30, barHeight));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int h = getHeight();
        int fillWidth = (int) (width * percentage / 100.0);

        g2.setColor(JBColor.border());
        g2.fillRoundRect(0, 0, width, h, h, h);

        if (fillWidth > 0) {
            g2.setColor(new JBColor(new Color(130, 130, 130), new Color(160, 160, 160)));
            g2.fillRoundRect(0, 0, fillWidth, h, h, h);
        }

        g2.dispose();
    }
}
