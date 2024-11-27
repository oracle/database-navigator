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

package com.dbn.common.icon;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;

public class CompositeIcon implements Icon {

    private final Icon leftIcon;
    private final Icon rightIcon;
    private final int horizontalStrut;

    public CompositeIcon(@NotNull Icon leftIcon, @NotNull Icon rightIcon, int horizontalStrut) {
        this.leftIcon = leftIcon;
        this.rightIcon = rightIcon;
        this.horizontalStrut = horizontalStrut;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        paintIconAlignedCenter(c, g, x, y, leftIcon);
        paintIconAlignedCenter(c, g, x + leftIcon.getIconWidth() + horizontalStrut, y, rightIcon);
    }

    private void paintIconAlignedCenter(Component c, Graphics g, int x, int y, @NotNull Icon icon) {
        int iconHeight = getIconHeight();
        icon.paintIcon(c, g, x, y + (iconHeight - icon.getIconHeight()) / 2);
    }

    @Override
    public int getIconWidth() {
        return leftIcon.getIconWidth() + horizontalStrut + rightIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return Math.max(leftIcon.getIconHeight(), rightIcon.getIconHeight());
    }
}