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

import com.dbn.common.ui.util.UserInterface;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * An implementation of the {@link FocusListener} repainting the component on focus events
 * (useful when components have a slightly different presentation when focused,
 *   but the presentation change is silent (e.g. list selection color)
 *
 * @@author Dan Cioca (Oracle)
 */
public class RepaintOnFocusChangeListener implements FocusListener {
    public static final RepaintOnFocusChangeListener INSTANCE = new RepaintOnFocusChangeListener();
    private RepaintOnFocusChangeListener() {}

    @Override
    public void focusGained(FocusEvent e) {
        UserInterface.repaint(e.getComponent());
    }

    @Override
    public void focusLost(FocusEvent e) {
        UserInterface.repaint(e.getComponent());
    }
}
