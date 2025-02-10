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

import javax.swing.JComboBox;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ComboBoxSelectionKeyListener extends KeyAdapter {
    private final JComboBox comboBox;
    private final boolean useControlKey;

    public static KeyListener create(JComboBox comboBox, boolean useControlKey) {
        return new ComboBoxSelectionKeyListener(comboBox, useControlKey);
    }

    private ComboBoxSelectionKeyListener(JComboBox comboBox, boolean useControlKey) {
        this.comboBox = comboBox;
        this.useControlKey = useControlKey;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isConsumed()) return;

        int operatorSelectionIndex = comboBox.getSelectedIndex();
        boolean controlled = e.isControlDown() || (e.getModifiersEx() & KeyEvent.META_DOWN_MASK) != 0;

        if ((useControlKey && controlled) || (!useControlKey && !controlled)) {
            if (e.getKeyCode() == 38) {//UP
                if (operatorSelectionIndex > 0) {
                    comboBox.setSelectedIndex(operatorSelectionIndex - 1);
                    UserInterface.repaint(comboBox);
                }
                e.consume();
            } else if (e.getKeyCode() == 40) { // DOWN
                if (operatorSelectionIndex < comboBox.getItemCount() - 1) {
                    comboBox.setSelectedIndex(operatorSelectionIndex + 1);
                    UserInterface.repaint(comboBox);
                }
                e.consume();
            }
        }
    }
}
