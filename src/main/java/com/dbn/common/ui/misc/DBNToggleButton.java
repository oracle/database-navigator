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

package com.dbn.common.ui.misc;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.ValueSelectorListener;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.RoundedCornerBorder;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.function.Function;

import static com.intellij.util.ui.UIUtil.getLabelForeground;
import static com.intellij.util.ui.UIUtil.getLabelSuccessForeground;

@Getter
@Setter
public class DBNToggleButton<T extends Presentable> extends JLabel {
    public static final Color FOREGROUND_DEFAULT_COLOR = ColorUtil.dimmer(getLabelForeground());
    public static final Color FOREGROUND_ERROR_COLOR = ColorUtil.desaturate(UIUtil.getErrorForeground(), 3);
    public static final Color FOREGROUND_SUCCESS_COLOR = ColorUtil.desaturate(getLabelSuccessForeground(), 3);

    private T[] values;
    private T selectedValue;
    private Function<T, Color> textColor = t -> getDefaultForeground();
    private boolean highlighted;
    private boolean focused;

    private final Listeners<ValueSelectorListener<T>> listeners = Listeners.create();

    public DBNToggleButton() {
        Mouse.onMouseClick(this, MouseEvent.BUTTON1, e -> selectNextValue(e.getClickCount()));
        addMouseListener(createMouseListener());
        addFocusListener(createFocusListener());
        Keyboard.onKeyPress(this, KeyEvent.VK_SPACE, e -> selectNextValue(1));
        setHorizontalAlignment(SwingConstants.CENTER);
        setFocusable(true);
    }

    private Mouse.Listener createMouseListener() {
        return Mouse.listener()
                .onEnter(e -> markHighlighted(true))
                .onExit(e -> markHighlighted(false));
    }

    private FocusListener createFocusListener() {
        return new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                markFocused(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                markFocused(false);
            }
        };
    }

    private void markHighlighted(boolean highlighted) {
        this.highlighted = highlighted && isEnabled();
        setBorder(createBorder());
    }

    private void markFocused(boolean focused) {
        this.focused = focused && isEnabled();
        setBorder(createBorder());

    }

    public void addListener(ValueSelectorListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(ValueSelectorListener<T> listener) {
        listeners.remove(listener);
    }

    public void setValues(T[] values) {
        this.values = values;
        setSelectedValue(values[0]);
    }

    private void selectNextValue(int count) {
        if (!isEnabled()) return;

        int valueIndex = ArrayUtil.indexOf(values, selectedValue);
        setSelectedValue(values[(valueIndex + count) % values.length]);
    }

    public void setSelectedValue(T value) {
        if (selectedValue == value) return;
        T oldValue = selectedValue;

        selectedValue = value;
        listeners.notify(l -> l.selectionChanged(oldValue, value));
        updateComponent();
    }

    private void updateComponent() {
        setBorder(createBorder());
        setForeground(getTextColor());
        setText(selectedValue == null ? "" : selectedValue.getName());
    }

    private Border createBorder() {
        Color color = getBorderColor();

        int margin = focused ? 1 : 2;
        int thickness = focused ? 2 : 1;
        Border outsideBorder = new RoundedCornerBorder(color, thickness, 6, margin);
        Border insideBorder = Borders.insetBorder(0, 8, 0, 8);
        return new CompoundBorder(outsideBorder, insideBorder);
    }

    @NotNull
    private Color getBorderColor() {
        Color outlineColor = Colors.getOutlineColor();
        return
            focused ? UIUtil.getFocusedBorderColor() :
            highlighted ? Colors.lafDarker(outlineColor, 10) :
            outlineColor;
    }

    private Color getTextColor() {
        Color color = textColor.apply(selectedValue);
        return color == null ? FOREGROUND_DEFAULT_COLOR : color;
    }

    public static Color getDefaultForeground() {
        return FOREGROUND_DEFAULT_COLOR;
    }

    public static Color getErrorForeground() {
        return FOREGROUND_ERROR_COLOR;
    }

    public static Color getSuccessForeground() {
        return FOREGROUND_SUCCESS_COLOR;
    }
}
