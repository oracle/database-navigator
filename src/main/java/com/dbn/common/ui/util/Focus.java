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

import com.dbn.common.routine.Consumer;
import lombok.experimental.UtilityClass;

import javax.swing.JComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Utility class for handling focus-related events on Swing components.
 * Provides methods to perform actions when a component gains or loses focus.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Focus {

    public static void onFocusGained(JComponent component, Consumer<FocusEvent> consumer) {
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                consumer.accept(e);
            }
        });
    }

    public static void onFocusLost(JComponent component, Consumer<FocusEvent> consumer) {
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                consumer.accept(e);
            }
        });
    }

}
