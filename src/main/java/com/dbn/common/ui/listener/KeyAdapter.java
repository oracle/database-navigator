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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public interface KeyAdapter extends KeyListener {
    /**
     * Invoked when a key has been typed.
     * This event occurs when a key press is followed by a key release.
     */
    default void keyTyped(KeyEvent e) {}

    /**
     * Invoked when a key has been pressed.
     */
    default void keyPressed(KeyEvent e) {}

    /**
     * Invoked when a key has been released.
     */
    default void keyReleased(KeyEvent e) {}
}
