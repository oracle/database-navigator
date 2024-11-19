package com.dbn.common.ui.tab;

import com.intellij.openapi.Disposable;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Function;

import static com.dbn.common.ui.util.ClientProperty.TAB_COLOR;
import static com.dbn.common.ui.util.ClientProperty.TAB_CONTENT;

public class DBNTabbedPane<T extends Disposable> extends DBNTabbedPaneBase<T> {
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
            int selectedIndex = source.getSelectedIndex();
            selectionListeners.notify(l -> l.selectionChanged(selectedIndex));
        });
    }

    public T getSelectedContent() {
        return getContentAt(getSelectedIndex());
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
