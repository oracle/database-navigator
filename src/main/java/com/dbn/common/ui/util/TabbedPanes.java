/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.dbn.common.dispose.Disposer;
import com.intellij.ui.components.JBTabbedPane;
import lombok.experimental.UtilityClass;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@UtilityClass
public class TabbedPanes {

    public static String getSelectedTabTitle(JBTabbedPane tabbedPane) {
        int index = tabbedPane.getSelectedIndex();
        if (index == -1) return "";
        return tabbedPane.getTitleAt(index);
    }

    public static void selectTab(JBTabbedPane tabbedPane, String title) {
        selectTab(tabbedPane, title, i -> tabbedPane.getTitleAt(i));
    }

    public static void selectTab(JBTabbedPane tabbedPane, JComponent component) {
        selectTab(tabbedPane, component, i -> tabbedPane.getComponentAt(i));
    }

/*    public static void selectTab(T content) {
        selectTab(content, i -> getContentAt(i));
    }*/

    public static void selectTab(JBTabbedPane tabbedPane, Component component, boolean requestFocus) {
        int index = getTabIndex(tabbedPane, component);
        selectTab(tabbedPane, index, requestFocus);
    }

    public static int getTabIndex(JBTabbedPane tabbedPane, Component component) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComponent = tabbedPane.getComponentAt(i);
            if (tabComponent == component) return i;
        }
        return -1;
    }

    public static void selectTab(JBTabbedPane tabbedPane, int index, boolean requestFocus) {
        tabbedPane.setSelectedIndex(index);
        if (requestFocus) {
            Component component = tabbedPane.getComponentAt(index);
            component.requestFocus();
        }
    }

    private static <E> void selectTab(JBTabbedPane tabbedPane, E element, Function<Integer, E> predicate) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            E elementAtIndex = predicate.apply(i);
            if (Objects.equals(elementAtIndex, element)) {
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }
    }

    public static void onSelectionChange(JBTabbedPane tabbedPane, Consumer<Integer> consumer) {
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index == -1) return;

            consumer.accept(index);
        });
    }

    public void removeAllTabs(JBTabbedPane tabbedPane) {
        while (tabbedPane.getTabCount() > 0) {
            Component component = tabbedPane.getComponent(0);
            tabbedPane.removeTabAt(0);
            Disposer.dispose(component);
        }
    }

}
