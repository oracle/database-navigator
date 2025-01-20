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

import com.dbn.common.Wrapper;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.DataProviderDelegate;
import com.dbn.common.action.DataProviders;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Actions;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBDimension;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.UIResource;
import java.awt.BorderLayout;
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
import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.ui.util.UserInterface.findChildComponent;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
@Slf4j
class DBNTabbedPaneBase<T extends Disposable> extends JBTabbedPane implements StatefulDisposable, DataProviderDelegate {
    private boolean disposed;
    private int popupTabIndex = -1;
    private JPanel hiddenTabsActionPanel;
    protected final Listeners<DBNTabsSelectionListener> selectionListeners = new Listeners<>();
    protected final Listeners<DBNTabsUpdateListener> updateListeners = new Listeners<>();

    public DBNTabbedPaneBase(int tabPlacement, Disposable parent, boolean mutable) {
        super(tabPlacement, JTabbedPane.SCROLL_TAB_LAYOUT);
        setUI(new DBNTabbedPaneUI());
        setTabComponentInsets(null);

        installHiddenTabButton();
        installTabCloser(mutable);

        DataProviders.register(this, this);
        Disposer.register(parent, this);
    }

    private void installHiddenTabButton() {
        add(hiddenTabsActionPanel = new HiddenTabsPanel());
    }

    private final class HiddenTabsPanel extends JPanel implements UIResource {
        public HiddenTabsPanel() {
            super(new BorderLayout());

            AnAction action = new AnAction(txt("app.shared.action.ShowHiddenTabs"), null, AllIcons.Actions.FindAndShowNextMatches) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    showHiddenTabsPopup(e.getDataContext());
                }
            };
            PresentationFactory presentationFactory = new PresentationFactory();
            Presentation presentation = presentationFactory.getPresentation(action);
            ActionButton actionButton = new ActionButton(
                    action,
                    presentation,
                    ActionPlaces.TOOLBAR,
                    new JBDimension(20, 20));

            actionButton.setBorder(Borders.insetBorder(4));
            add(actionButton);
        }
    }

    private void showHiddenTabsPopup(DataContext dataContext) {
        DBNTabbedPaneUI ui = (DBNTabbedPaneUI) getUI();
        List<Integer> indexes = ui.getHiddenTabIndexes();

        List<AnAction> actions = new ArrayList<>();
        for (int index : indexes) {
            String title = getTitleAt(index);
            title = Actions.adjustActionName(title);
            Icon icon = getIconAt(index);
            actions.add(new AnAction(title, null, icon) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    setSelectedIndex(index);
                }
            });
        }

        popupBuilder(actions, dataContext).
                withTitle("Hidden Tabs").
                withTitleVisible(false).
                withSpeedSearch().
                buildAndShow();
    }

    private void installTabCloser(boolean mutable) {
        if (!mutable) return;

        addMouseListener(Mouse.listener().onClick(e -> {
            int index = indexAtLocation(e.getX(), e.getY());
            if (index == -1) return;

            if (isCloseTabEvent(e)) {
                removeTabAt(index);
                e.consume();
            } else if (isSelectTabEvent(e)) {
                focusTab(index);
            }
        }));
    }

    private void focusTab(int index) {
        Component content = getComponentAt(index);
        JComponent focusable = findChildComponent(content, c -> c.isFocusable());
        if (focusable != null)  focusable.requestFocus();
    }

    private static boolean isCloseTabEvent(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) return true;
        if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) return true;

        return false;
    }

    private static boolean isSelectTabEvent(MouseEvent e) {
        return SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1;
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

    @Nullable
    public final Component getSelectedTabComponent() {
        int index = getSelectedIndex();
        if (index == -1) return null;

        return getComponentAt(index);
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

        ListPopup popup = popupBuilder(actionGroup, this).
                withTitle("Tab Actions").
                withTitleVisible(false).
                withDisposeCallback(() -> popupTabIndex = -1).
                withMaxRowCount(10).
                build();

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
