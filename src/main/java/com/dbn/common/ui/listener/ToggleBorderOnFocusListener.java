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

package com.dbn.common.ui.listener;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Component focus listener that toggles the border when component receives or loses focus.
 * Useful to highlight focus on custom components which do not feature this by default, like the JTextField
 *
 * @author Dan Cioca (Oracle)
 */
public class ToggleBorderOnFocusListener implements FocusListener {
    private final Border unfocusedBorder;
    private final Border focusedBorder;

    public ToggleBorderOnFocusListener(Border unfocusedBorder, Border focusedBorder) {
        this.focusedBorder = focusedBorder;
        this.unfocusedBorder = unfocusedBorder;
    }

    @Override
    public void focusGained(FocusEvent e) {
        changeBorder(e, focusedBorder);
    }

    @Override
    public void focusLost(FocusEvent e) {
        changeBorder(e, unfocusedBorder);
    }

    private static void changeBorder(FocusEvent e, Border border) {
        Component component = e.getComponent();
        if (component instanceof JComponent) {
            JComponent comp = (JComponent) component;
            comp.setBorder(border);
        }
    }
}
