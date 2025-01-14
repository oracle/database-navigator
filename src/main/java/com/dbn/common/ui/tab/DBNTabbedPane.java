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

package com.dbn.common.ui.tab;

import com.intellij.openapi.Disposable;
import com.intellij.util.ui.JBInsets;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.util.Objects;
import java.util.function.Function;

import static com.dbn.common.ui.util.ClientProperty.FOCUS_INHERITANCE;
import static com.dbn.common.ui.util.ClientProperty.TAB_COLOR;
import static com.dbn.common.ui.util.ClientProperty.TAB_CONTENT;
import static com.dbn.common.ui.util.UserInterface.hasChildComponent;

public class DBNTabbedPane<T extends Disposable> extends DBNTabbedPaneBase<T> {
    public static final Insets REGULAR_INSETS = new JBInsets(6, 6, 6, 6);

    public DBNTabbedPane(Disposable parent) {
        this(parent, false);
    }

    public DBNTabbedPane(Disposable parent, boolean mutable) {
        this(TOP, parent, mutable);
    }

    public DBNTabbedPane(int tabPlacement, Disposable parent, boolean mutable) {
        super(tabPlacement, parent, mutable);

        addChangeListener(e -> {
            DBNTabbedPane source = (DBNTabbedPane) e.getSource();
            int index = source.getSelectedIndex();
            if (index == -1) return;

            selectionListeners.notify(l -> l.selectionChanged(index));
        });
    }

    public void enableFocusInheritance() {
        FOCUS_INHERITANCE.set(this, true);
    }

    public boolean hasFocusInheritance() {
        return FOCUS_INHERITANCE.is(this);
    }

    public boolean hasInheritedFocus() {
        if (!hasFocusInheritance()) return false;

        Component selectedComponent = getSelectedComponent();
        if (selectedComponent == null) return false;

        if (selectedComponent instanceof JComponent) {
            JComponent component = (JComponent) selectedComponent;
            return hasChildComponent(component, JComponent.class, c -> c.hasFocus());
        }
        return false;
    }

    @Nullable
    public T getSelectedContent() {
        int index = getSelectedIndex();
        if (index == -1) return null;

        return getContentAt(index);
    }

    public T getContentAt(int index) {
        Component component = getComponentAt(index);
        return TAB_CONTENT.get(component);
    }

    public Color getTabColorAt(int index) {
        Component component = getComponentAt(index);
        return TAB_COLOR.get(component);
    }

    public void setTabIcon(Component component, Icon icon) {
        int index = getTabIndex(component);
        setIconAt(index, icon);
    }

    public void setTabTitle(Component component, String title) {
        int index = getTabIndex(component);
        setTitleAt(index, title);
    }

    public void setTabColor(Component component, Color color) {
        int index = getTabIndex(component);
        setTabColorAt(index, color);
    }

    public void setTabColorAt(int index, Color color) {
        Component component = getComponentAt(index);
        TAB_COLOR.set(component, color);
    }

    public void addTabSelectionListener(DBNTabsSelectionListener listener) {
        selectionListeners.add(listener);
    }

    public void addTabUpdateListener(DBNTabsUpdateListener listener) {
        updateListeners.add(listener);
    }

    public String getSelectedTabTitle() {
        int index = getSelectedIndex();
        if (index == -1) return "";
        return getTitleAt(index);
    }

    public void selectTab(String title) {
        selectTab(title, i -> getTitleAt(i));
    }

    public void selectTab(JComponent component) {
        selectTab(component, i -> getComponentAt(i));
    }

    public void selectTab(T content) {
        selectTab(content, i -> getContentAt(i));
    }

    public void selectTab(Component component, boolean requestFocus) {
        int index = getTabIndex(component);
        selectTab(index, requestFocus);
    }

    public void selectTab(int index, boolean requestFocus) {
        setSelectedIndex(index);
        if (requestFocus) {
            Component component = getComponentAt(index);
            component.requestFocus();
        }
    }

    private <E> void selectTab(E element, Function<Integer, E> predicate) {
        for (int i = 0; i < getTabCount(); i++) {
            E elementAtIndex = predicate.apply(i);
            if (Objects.equals(elementAtIndex, element)) {
                setSelectedIndex(i);
                return;
            }
        }
    }
}
