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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

@UtilityClass
public class Keyboard {
    public interface Key {
        int ENTER = 10;
        int ESCAPE = 27;
        int DELETE = 127;
    }

    public static boolean match(Shortcut[] shortcuts, AnActionEvent e) {
        InputEvent inputEvent = e.getInputEvent();
        if (inputEvent instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) inputEvent;
            return match(shortcuts, keyEvent);
        }
        return false;
    }

    public static boolean match(Shortcut[] shortcuts, KeyEvent e) {
        for (Shortcut shortcut : shortcuts) {
            if (shortcut instanceof KeyboardShortcut) {
                KeyboardShortcut keyboardShortcut = (KeyboardShortcut) shortcut;
                KeyStroke shortkutKeyStroke = keyboardShortcut.getFirstKeyStroke();
                KeyStroke eventKeyStroke = KeyStroke.getKeyStrokeForEvent(e);
                if (shortkutKeyStroke.equals(eventKeyStroke)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean match(AnAction action, KeyEvent e) {
        return match(action.getShortcutSet().getShortcuts(), e);
    }

    public static Shortcut[] getShortcuts(String actionId) {
        return KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionId);
    }

    public static ShortcutSet createShortcutSet(int keyCode, int modifiers) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        Shortcut shortcut = new KeyboardShortcut(keyStroke, null);
        return new CustomShortcutSet(shortcut);
    }

    public static boolean isEmacsKeymap() {
        return KeymapUtil.isEmacsKeymap();
    }

    public static boolean isEmacsKeymap(@Nullable Keymap keymap) {
        return KeymapUtil.isEmacsKeymap(keymap);
    }

    /**
     * Adds a key press listener to the specified component. When the specified key is pressed,
     * the provided consumer is invoked with the key event.
     *
     * @param component   the JComponent to which the key press listener is added
     * @param keyCode     the key code of the key to listen for
     * @param keyConsumer the consumer that processes the key event when the specified key is pressed
     */
    public static void onKeyPress(JComponent component, int keyCode, Consumer<KeyEvent> keyConsumer) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == keyCode) {
                    keyConsumer.accept(e);
                }
            }
        });
    }
}
