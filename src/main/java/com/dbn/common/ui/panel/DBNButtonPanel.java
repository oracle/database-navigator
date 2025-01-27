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

package com.dbn.common.ui.panel;

import com.dbn.common.ui.listener.ToggleBorderOnFocusListener;
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder;
import com.intellij.util.ui.JBUI;
import lombok.Setter;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.LayoutManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import static com.dbn.common.ui.util.Keyboard.onKeyPress;
import static com.dbn.common.ui.util.Mouse.onMouseClick;

/**
 * A custom Swing panel for button-like functionality, designed with focus and click handling.
 * <br>
 * This class extends JPanel and provides specific behaviors to simulate a button,
 * including focus-based border changes and action handling for mouse clicks and
 * spacebar key presses. The actions are executed using a customizable Consumer.
 * <br>
 * Accessibility support is provided, with the panel being identified as a "push button".
 * <br>
 * Key Features:
 * - Focusable: Handles focus events and changes its border to indicate focus status.
 * - Action Handling: Listens for left mouse button clicks and spacebar key presses,
 *   and allows custom action execution via an actionConsumer.
 * - Accessibility: Defined as a push button for assistive technologies using AccessibleContext.
 *
 * This panel is opaque by default, with pre-defined borders for normal and focused states.
 *
 * @author Dan Cioca (Oracle)
 */
@Setter
public class DBNButtonPanel extends JPanel {
    public static final Border DEFAULT_BORDER = JBUI.Borders.empty(3);
    public static final Border FOCUS_BORDER = new DarculaTextBorder();
    private Consumer<InputEvent> actionConsumer = inputEvent -> {};

    public DBNButtonPanel() {
        init();
    }

    public DBNButtonPanel(LayoutManager layout) {
        super(layout);
        init();
    }

    private void init() {
        setFocusable(true);
        setRequestFocusEnabled(true);
        setBorder(DEFAULT_BORDER);
        setOpaque(false);

        addFocusListener(new ToggleBorderOnFocusListener(DEFAULT_BORDER, FOCUS_BORDER));
        onMouseClick(this, MouseEvent.BUTTON1, 1, e -> actionConsumer.accept(e));
        onKeyPress(this, KeyEvent.VK_SPACE, e -> actionConsumer.accept(e));
    }


    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJPanel() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }
}
