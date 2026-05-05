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
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.util.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import static com.dbn.common.Reflection.invokeMethod;

@Workaround // internal editor tabs
public class DBNColoredTabs<T extends DBNForm> extends JBEditorTabs {
    public DBNColoredTabs(@NotNull Disposable parentDisposable) {
        super(null, parentDisposable);
    }

    public void addTab(String title, JComponent component) {
        TabInfo tabInfo = new TabInfo(component);
        tabInfo.setText(title);

        //addTab(tabInfo);
        invokeMethod(this, "addTab", tabInfo);
    }

    public void setTabColor(Component component, Color color) {
        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;

        tabInfo.setTabColor(color);
    }

    public void setTabTitle(Component component, String title) {
        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;

        tabInfo.setText(title);

    }

    public void selectTab(T form) {
        TabInfo tabInfo = getTabInfo(form.getComponent());
        if (tabInfo == null) return;

        //select(tabInfo, true);
        invokeMethod(this, "select", tabInfo, true);

    }

    private TabInfo getTabInfo(Component component) {
        //List<TabInfo> tabInfos = getTabs();
        List<TabInfo> tabInfos = invokeMethod(this, "getTabs");
        if (tabInfos == null) return null;

        for (TabInfo tabInfo : tabInfos) {
            if (tabInfo.getComponent() == component) {
                return tabInfo;
            }
        }
        return null;
    }

    public Component getSelectedTabComponent() {
        //TabInfo tabInfo = getSelectedInfo();
        TabInfo tabInfo = invokeMethod(this, "getSelectedInfo");
        if (tabInfo == null) return null;

        return tabInfo.getComponent();
    }

    public List<JComponent> getTabbedComponents() {
        //List<TabInfo> tabInfos = getTabs();
        List<TabInfo> tabInfos = invokeMethod(this, "getTabs");
        return Lists.convert(tabInfos, i -> i.getComponent());
    }

    public void removeTab(Component component, boolean dispose) {
        TabInfo tabInfo = getTabInfo(component);
        if (tabInfo == null) return;


    }

    public void onSelectionChange(Runnable runnable) {
        //addListener(createSelectionListener(runnable));
        invokeMethod(this, "addListener", createSelectionListener(runnable));
    }

    private @NotNull TabsListener createSelectionListener(Runnable runnable) {
        return new TabsListener() {
            @Override
            public void selectionChanged(@Nullable TabInfo oldSelection, @Nullable TabInfo newSelection) {
                runnable.run();
            }
        };
    }
}
