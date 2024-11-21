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

package com.dbn.common.ui.util;

import com.dbn.common.color.Colors;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;
import lombok.experimental.UtilityClass;

import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Insets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public final class Borders {

    public static final Insets EMPTY_INSETS = JBUI.emptyInsets();
    public static final Border EMPTY_BORDER = new EmptyBorder(EMPTY_INSETS);

    public static final Border TEXT_FIELD_INSETS = JBUI.Borders.empty(0, 3);

    public static final Border TOP_LINE_BORDER = new CustomLineBorder(Colors.getOutlineColor(),1,0, 0,0);
    public static final Border BOTTOM_LINE_BORDER = new CustomLineBorder(Colors.getOutlineColor(),0,0, 1,0);
    public static final Border COMPONENT_OUTLINE_BORDER = new LineBorder(Colors.getOutlineColor(),1);
    public static final Border TOOLBAR_DECORATOR_BORDER = new CustomLineBorder(Colors.getOutlineColor(),1, 1, 0, 1);

    private static final Map<Color, Border> LINE_BORDERS = new ConcurrentHashMap<>();
    private static final Map<Integer, Border> INSET_BORDERS = new ConcurrentHashMap<>();
    private static final Map<Integer, Border> TOP_INSET_BORDERS = new ConcurrentHashMap<>();

    public static Border lineBorder(Color color) {
        return LINE_BORDERS.computeIfAbsent(color, c -> new LineBorder(c, 1));
    }

    public static Border lineBorder(Color color, int top, int left, int bottom, int right) {
        return new CustomLineBorder(color, top, left, bottom, right);
    }

    public static Border lineBorder(Color color, int thickness) {
        return new LineBorder(color, thickness);
    }

    public static Border insetBorder(int insets) {
        return INSET_BORDERS.computeIfAbsent(insets, i -> new EmptyBorder(JBUI.insets(i)));
    }

    public static Border insetBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(JBUI.insets(top, left, bottom, right));
    }

    public static Border topInsetBorder(int inset) {
        return TOP_INSET_BORDERS.computeIfAbsent(inset, i -> new EmptyBorder(JBUI.insets(i, 0, 0, 0)));
    }

    public static Border buttonBorder() {
        Border insideBorder = JBUI.Borders.empty(0, 8);
        Border textFieldBorder = UIManager.getBorder("TextField.border");
        return new CompoundBorder(textFieldBorder, insideBorder);
    }

    public static Border tableBorder(int top, int left, int bottom, int right) {
        return lineBorder(Colors.getTableHeaderGridColor(), top, left, bottom, right);
    }
}
