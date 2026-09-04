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

package com.dbn.common.ui.tab;

import com.dbn.common.compatibility.Workaround;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.JBTabsPresentation;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.UiDecorator.UiDecoration;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.dbn.common.Reflection.invokeMethod;
import static com.dbn.nls.NlsResources.txt;
import static org.jetbrains.annotations.Nls.Capitalization.Title;

@Getter
@Setter
@Workaround // internal editor tabs
public class DBNColoredTabs<T extends DBNForm> extends JBEditorTabs {
    private boolean closeable;

    public DBNColoredTabs(@NotNull DBNForm parentForm) {
        super(parentForm.ensureProject(), IdeFocusManager.getGlobalInstance(), parentForm);

        initTabsPresentation();
        initTabMouseListener();
    }

    private void initTabMouseListener() {
        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!closeable) return;
                if (e.getButton() != MouseEvent.BUTTON2) return;
                if ( e.getClickCount() != 1) return;

                //TabInfo tabInfo = findInfo(e);
                TabInfo tabInfo = invokeMethod(DBNColoredTabs.this, "findInfo", e);
                if (tabInfo == null) return;

                closeTab(tabInfo);
                e.consume();
            }
        };
        invokeMethod(this, "addTabMouseListener", listener);
    }

    private void initTabsPresentation() {
        //JBTabsPresentation presentation = getPresentation();
        JBTabsPresentation presentation = invokeMethod(this, "getPresentation");
        if (presentation == null) return;

        presentation.setUiDecorator(() -> new UiDecoration(JBUI.Fonts.label(), JBUI.insets(8)));

    }

    private void initTabActions(TabInfo tabInfo) {
        if (closeable) {
            DefaultActionGroup tabActions = new DefaultActionGroup();
            tabActions.add(new CloseTabAction(tabInfo));
            tabInfo.setTabLabelActions(tabActions, "ColoredTabs");
        }
    }

    public void addTab(@Nls(capitalization = Title) String title, JComponent component) {
        TabInfo tabInfo = new TabInfo(component);
        setTabTitle(title, tabInfo);
        initTabActions(tabInfo);

        //addTab(tabInfo);
        invokeMethod(this, "addTab", tabInfo);
    }

    public void insertTab(@Nls(capitalization = Title) String title, JComponent component, int index) {
        TabInfo tabInfo = new TabInfo(component);
        setTabTitle(title, tabInfo);
        initTabActions(tabInfo);

        //addTab(tabInfo, index);
        invokeMethod(this, "addTab", tabInfo, index);
    }

    public void setTabColor(Component component, Color color) {
        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;

        tabInfo.setTabColor(color);
    }

    public void setTabTitle(Component component, @Nls(capitalization = Title) String title) {
        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;

        setTabTitle(title, tabInfo);

    }

    private static void setTabTitle(@Nls(capitalization = Title) String title, TabInfo tabInfo) {
        title = normalizeTitle(title);
        tabInfo.setText(title);
    }

    public void selectTab(T form, boolean requestFocus) {
        selectTab(form.getComponent(), requestFocus);
    }

    public void selectTab(JComponent component, boolean requestFocus) {
        if (!requestFocus && getSelectedTabComponent() == component) return;

        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;

        selectTab(tabInfo, requestFocus);
    }

    private void selectTab(TabInfo tabInfo, boolean requestFocus) {
        //select(tabInfo, requestFocus);
        invokeMethod(this, "select", tabInfo, requestFocus);
    }

    private TabInfo getTabInfo(Component component) {
        List<TabInfo> tabInfos = getTabInfos();

        for (TabInfo tabInfo : tabInfos) {
            if (tabInfo.getComponent() == component) {
                return tabInfo;
            }
        }
        return null;
    }

    private List<TabInfo> getTabInfos() {
        //List<TabInfo> tabInfos = getTabs();
        List<TabInfo> tabInfos = invokeMethod(this, "getTabs");
        return tabInfos == null ? Collections.emptyList() : tabInfos;
    }

    private TabInfo getTabInfo(int tabIndex) {
        List<TabInfo> tabInfos = getTabInfos();
        if (tabInfos.size() <= tabIndex) return null;
        return tabInfos.get(tabIndex);
    }

    public Component getSelectedTabComponent() {
        //TabInfo tabInfo = getSelectedInfo();
        TabInfo tabInfo = invokeMethod(this, "getSelectedInfo");
        if (tabInfo == null) return null;

        return tabInfo.getComponent();
    }

    public List<JComponent> getTabbedComponents() {
        List<TabInfo> tabInfos = getTabInfos();
        return Lists.convert(tabInfos, i -> i.getComponent());
    }

    public void closeTab(Component component) {
        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;

        closeTab(tabInfo);
    }

    private void closeTab(TabInfo tabInfo) {
        //removeTab(tabInfo);
        invokeMethod(this, "removeTab", tabInfo);
        disposeTabContent(tabInfo);
    }

    private static void disposeTabContent(TabInfo tabInfo) {
        JComponent component = tabInfo.getComponent();
        DBNForm form = ClientProperty.FORM.get(component);
        Disposer.dispose(form);
    }

    public void onTabSelected(Consumer<Integer> consumer) {
        //addListener(createTabSelectedListener(consumer));
        invokeMethod(this, "addListener", createTabSelectedListener(consumer));
    }

    public void onTabRemoved(Runnable runnable) {
        //addListener(createTabRemovedListener(runnable));
        invokeMethod(this, "addListener", createTabRemovedListener(runnable));
    }


    private @NotNull TabsListener createTabSelectedListener(Consumer<Integer> consumer) {
        return new TabsListener() {
            @Override
            public void selectionChanged(@Nullable TabInfo oldSelection, @Nullable TabInfo newSelection) {
                //int tabIndex = getIndexOf(newSelection);
                Integer tabIndex = invokeMethod(DBNColoredTabs.this, "getIndexOf", newSelection);
                consumer.accept(tabIndex);
            }
        };
    }

    private @NotNull TabsListener createTabRemovedListener(Runnable runnable) {
        return new TabsListener() {
            @Override
            public void tabRemoved(@NotNull TabInfo tabToRemove) {
                runnable.run();
            }
        };
    }

    public Component getTabComponent(Integer i) {
        TabInfo tabInfo = getTabInfo(i);
        if (tabInfo == null) return null;

        return tabInfo.getComponent();
    }

    public int getTabsCount() {
        Integer tabCount = invokeMethod(this, "getTabCount");
        return tabCount == null ? 0 : tabCount;
    }

    public void setTabIcon(Component component, Icon icon) {
        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;

        tabInfo.setIcon(icon);
    }

    public int getTabIndex(JComponent component) {
        List<TabInfo> tabInfos = getTabInfos();
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo tabInfo = tabInfos.get(i);
            if (tabInfo.getComponent() == component) return i;
        }

        return -1;
    }

    public void setSelectedIndex(int index) {
        TabInfo tabInfo = getTabInfo(index);
        selectTab(tabInfo, false);
    }

    public void setTabsLocation(JBTabsPosition tabsPosition) {
        //setTabsPosition(tabsPosition);
        invokeMethod(this, "setTabsPosition", tabsPosition);
    }

    public Component getPopupTabComponent() {
        //TabInfo tabInfo = getTargetInfo();
        TabInfo tabInfo = invokeMethod(this, "getTargetInfo");
        if (tabInfo == null) return null;

        return tabInfo.getComponent();
    }

    public void setPopupActions(ActionGroup actionGroup, String actionPlace, boolean addNavigationActions) {
        //setPopupGroup(actionGroup, actionPlace, addNavigationActions);
        invokeMethod(this, "setPopupGroup", actionGroup, actionPlace, addNavigationActions);
    }

    private class CloseTabAction extends DumbAwareAction {
        private final TabInfo tabInfo;

        private CloseTabAction(TabInfo tabInfo) {
            this.tabInfo = tabInfo;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (e.getInputEvent() instanceof MouseEvent && BitUtil.isSet(e.getInputEvent().getModifiersEx(), InputEvent.ALT_DOWN_MASK)) {
                closeAllTabsExceptCurrent();
            }
            else {
                closeCurrentTab();
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setIcon(AllIcons.Actions.Close);
            presentation.setHoveredIcon(AllIcons.Actions.CloseHovered);
            presentation.setVisible(true);
            presentation.setText(txt("app.shared.action.CloseTab", SystemInfo.isMac ? "⌥" : "Alt+"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        private void closeCurrentTab() {
            closeTab(tabInfo);
        }

        private void closeAllTabsExceptCurrent() {
            getTabInfos().stream()
                    .filter(tabInfo -> tabInfo != this.tabInfo)
                    .forEach(tabInfo -> closeTab(tabInfo));
        }
    }

    protected static String normalizeTitle(String title) {
        // prevent html contents in tab titles (BUGDB-38885384)
        return Strings.removeHtmlTags(title);
    }
}
