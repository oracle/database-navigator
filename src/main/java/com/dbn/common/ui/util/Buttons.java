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

import com.dbn.common.compatibility.Workaround;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Dispatch;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBOptionButton;
import lombok.experimental.UtilityClass;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

import static com.dbn.common.ui.util.Mouse.onMousePress;
import static com.dbn.common.ui.util.UserInterface.visitRecursively;

@UtilityClass
public class Buttons {

    public static void onButtonClick(JButton button, Consumer<ActionEvent> consumer) {
        button.addActionListener(e -> consumer.accept(e));
    }

    public static <T> void onButtonClickAsync(JButton button, Supplier<T> supplier, Consumer<T> consumer) {
        button.addActionListener(e -> clickButtonAsync(button, supplier, consumer));
    }

    public static <T> void clickButtonAsync(JButton button, Supplier<T> supplier, Consumer<T> consumer) {
        Supplier<T> interceptedSupplier = () -> {
            button.setEnabled(false);

            Icon originalIcon = button.getIcon();
            button.setIcon(new AnimatedIcon.Default());

            try {
                return supplier.get();
            } finally {
                button.setIcon(originalIcon);
                button.setEnabled(true);
            }
        };
        Dispatch.async(button, interceptedSupplier, consumer);
    }

    /**
     * Requests focus on the option button when any part of it is pressed.
     * <p>
     * {@link JBOptionButton} renders the main action and the arrow as nested, non-focusable buttons.
     * Mouse presses are delivered to those child buttons, while keyboard traversal focuses the parent
     * option button. Installing the listener recursively keeps mouse and keyboard focus behavior aligned.
     */
    @Workaround
    public static void installMousePressFocus(JBOptionButton button) {
        visitRecursively(button, c ->
                onMousePress(c, MouseEvent.BUTTON1, e -> {
                    if (!button.isEnabled()) return;
                    button.requestFocusInWindow(FocusEvent.Cause.MOUSE_EVENT);
                }));
    }
}
