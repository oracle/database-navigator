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

import com.intellij.util.ui.ScrollUtil;
import lombok.experimental.UtilityClass;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import java.awt.event.ActionEvent;

@UtilityClass
public class ScrollPanes {
    public static void scrollDown(JScrollPane scrollPane, boolean animate) {
        scrollPane.revalidate();
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        if (animate) {
            Timer timer = new Timer(20, e -> scrollDown(e, scrollBar, 50));
            timer.start();
        } else {
            int value = scrollBar.getMaximum() - scrollBar.getVisibleAmount();
            if (value > scrollBar.getValue()) {
                ScrollUtil.scrollVertically(scrollBar, value);
            }
        }
    }

    private static void scrollDown(ActionEvent e, JScrollBar scrollBar, int step) {
        int currentValue = scrollBar.getValue();
        int maximumValue = scrollBar.getMaximum() - scrollBar.getVisibleAmount();

        if (currentValue + step < maximumValue) {
            scrollBar.setValue(currentValue + step);
        } else {
            scrollBar.setValue(maximumValue); // Scroll to the bottom
            Timer timer = (Timer) e.getSource();
            timer.stop();
        }
    }
}
