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

package com.dbn.data.editor.ui;

import com.dbn.common.ui.util.Keyboard;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.Shortcut;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

public interface TextFieldPopupProvider extends Disposable{
    TextFieldPopupType getPopupType();

    void setEnabled(boolean enabled);

    void setButton(@Nullable JLabel button);

    @Nullable
    JLabel getButton();

    String getDescription();

    String getKeyShortcutDescription();

    @Nullable
    Icon getButtonIcon();

    Shortcut[] getShortcuts();

    boolean isButtonVisible();

    boolean isEnabled();

    boolean isAutoPopup();

    boolean isShowingPopup();

    void showPopup();

    void hidePopup();

    default void handleFocusLostEvent(FocusEvent focusEvent) {}

    default void handleKeyPressedEvent(KeyEvent keyEvent) {}

    default void handleKeyReleasedEvent(KeyEvent keyEvent) {}

    default boolean matchesKeyEvent(KeyEvent keyEvent) {
        return Keyboard.match(getShortcuts(), keyEvent);
    }
}
