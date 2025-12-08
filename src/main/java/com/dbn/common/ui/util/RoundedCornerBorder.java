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

package com.dbn.common.ui.util;

import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

public class RoundedCornerBorder extends AbstractBorder {
    private final Color color;
    private final int thickness;
    private final int radius;
    private final int margin;

    public RoundedCornerBorder(Color color, int thickness, int radius, int margin) {
        this.color = color;
        this.thickness = thickness;
        this.radius = radius;
        this.margin = margin;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(thickness));
        g2d.drawRoundRect(
                x + thickness / 2 + margin,
                y + thickness / 2 + margin,
                width - thickness - margin * 2,
                height - thickness - margin * 2, radius, radius);
        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        int inset = thickness + radius / 2 + margin;
        return new Insets(inset, inset, inset, inset);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.right = thickness + radius / 2 + margin;
        insets.top = insets.bottom = thickness + radius / 2 + margin;
        return insets;
    }
}