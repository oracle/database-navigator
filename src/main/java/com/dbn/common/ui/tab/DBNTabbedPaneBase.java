package com.dbn.common.ui.tab;

import com.dbn.common.Wrapper;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.DataProviderDelegate;
import com.dbn.common.action.DataProviders;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Context;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.components.JBTabbedPane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.ClientProperty.TAB_CONTENT;
import static com.dbn.common.ui.util.ClientProperty.TAB_ICON;
import static com.dbn.common.ui.util.ClientProperty.TAB_TOOLTIP;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@Slf4j
class DBNTabbedPaneBase<T extends Disposable> extends JBTabbedPane implements StatefulDisposable, DataProviderDelegate {
    private boolean disposed;
    private int popupTabIndex = -1;
    protected final Listeners<DBNTabsSelectionListener> selectionListeners = new Listeners<>();
    protected final Listeners<DBNTabsUpdateListener> updateListeners = new Listeners<>();

    public DBNTabbedPaneBase(int tabPlacement, Disposable parent, boolean mutable) {
        super(tabPlacement, JTabbedPane.SCROLL_TAB_LAYOUT);
        setUI(new DBNTabbedPaneUI());

        DataProviders.register(this, this);
        Disposer.register(parent, this);

        installTabCloser(mutable);

    }

    private void installTabCloser(boolean mutable) {
        if (!mutable) return;

        addMouseListener(Mouse.listener().onClick(e -> {
            int index = indexAtLocation(e.getX(), e.getY());
            if (index == -1) return;

            if (isCloseTabEvent(e)) {
                removeTabAt(index);
                e.consume();
            }
        }));
    }

    private static boolean isCloseTabEvent(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) return true;
        if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) return true;

        return false;
    }

    @Override
    public void updateUI() {
        setUI(new DBNTabbedPaneUI());
    }

    @Workaround // see assumption in BasicTabbedPaneUI.scrollableTabLayoutEnabled()
    public LayoutManager getLayout() {
        LayoutManager layout = super.getLayout();
        if (layout instanceof Wrapper) {
            Wrapper wrapped = (Wrapper) layout;
            return cast(wrapped.unwrap());
        }
        return layout;
    }


    public void insertTab(String title, Component component, int index) {
        Icon icon = TAB_ICON.get(component);
        String tooltip = TAB_TOOLTIP.get(component);
        insertTab(title, icon, component, tooltip, index);
    }

    @Workaround // remove tab label from JBTabbedPane
    public void insertTab(@Nls(capitalization = Nls.Capitalization.Title) String title, Icon icon, Component component, @Nls(capitalization = Nls.Capitalization.Sentence) String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        setTabComponentAt(index, null);
        updateListeners.notify(l -> l.tabAdded(index));
    }

    @Override
    public void addTab(String title, Icon icon, Component component, String tooltip) {
        TAB_ICON.set(component, icon);
        TAB_TOOLTIP.set(component, tooltip);
        super.addTab(title, icon, component, tooltip);

    }

    @Override
    public void addTab(String title, Icon icon, Component component) {
        TAB_ICON.set(component, icon);
        super.addTab(title, icon, component);
    }

    @Override
    public void addTab(String title, Component component) {
        Icon icon = TAB_ICON.get(component);
        String tooltip = TAB_TOOLTIP.get(component);
        super.addTab(title, icon, component, tooltip);
    }

    public void addTab(String title, Component component, T content) {
        Icon icon = TAB_ICON.get(component);
        String tooltip = TAB_TOOLTIP.get(component);
        TAB_CONTENT.set(component, content);

        addTab(title, icon, component, tooltip);
    }

    public final Component getSelectedTabComponent() {
        int selectedIndex = getSelectedIndex();
        return getComponentAt(selectedIndex);
    }

    public void removeTab(Component component, boolean disposeContent) {
        int index = getTabIndex(component);
        removeTabAt(index);

        if (disposeContent) {
            T content = TAB_CONTENT.get(component);
            Disposer.dispose(content);
        }
    }



    @Override
    public void removeTabAt(int index) {
        Component component = getComponentAt(index);
        T content = TAB_CONTENT.get(component);

        super.removeTabAt(index);
        updateListeners.notify(l -> l.tabRemoved(index));
        Disposer.dispose(content);
    }

    public void removeAllTabs() {
        while (getTabCount() > 0) {
            removeTabAt(0);
        }

    }

    public void disposeInner() {
        for (Component component : getTabbedComponents()) {
            Object content = TAB_CONTENT.get(component);
            Disposer.dispose(content);
        }
    }

    public void addTabMouseListener(MouseListener mouseListener) {
        addMouseListener(mouseListener);
        // TODO
    }

    public int getTabIndex(Component component) {
        for (int i=0; i<getTabCount(); i++) {
            Component tabComponent = getComponentAt(i);
            if (tabComponent == component) return i;
        }
        return -1;
    }

    public List<Component> getTabbedComponents() {
        List<Component> components = new ArrayList<>();
        int count = getTabCount();
        for (int i=0; i<count; i++) {
            Component component = getComponentAt(i);
            components.add(component);
        }
        return components;
    }

    public void setPopupActions(ActionGroup actionGroup) {
        Mouse.insertMouseListener(this, Mouse.listener().onPress(e -> {
            if (!SwingUtilities.isRightMouseButton(e)) return;

            int tabIndex = indexAtLocation(e.getX(), e.getY());
            if (tabIndex == -1) return;

            popupTabIndex = tabIndex;
            //setSelectedIndex(tabIndex);
            e.consume();
            showPopup(actionGroup, e.getX(), e.getY());
        }));
    }

    private void showPopup(ActionGroup actionGroup, int x, int y) {
        Point location = getLocationOnScreen();
        location.setLocation(location.getX() + x, location.getY() + y);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                actionGroup,
                Context.getDataContext(this),
                false,
                false,
                false,
                () -> popupTabIndex = -1,
                10,
                a -> false);

        popup.showInScreenCoordinates(this, location);
    }

    @Override
    public Object getData(String dataId) {
        if (DataKeys.TABBED_PANE.is(dataId)) return this;

        return null;
    }

    public boolean isShowingPopup() {
        return popupTabIndex > -1;
    }
}
