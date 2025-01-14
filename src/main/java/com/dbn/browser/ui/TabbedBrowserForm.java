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

package com.dbn.browser.ui;

import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.ui.tab.DBNTabs.initTabComponent;
import static com.dbn.common.ui.util.ClientProperty.TAB_CONTENT;

public class TabbedBrowserForm extends DatabaseBrowserForm{
    private final DBNTabbedPane<SimpleBrowserForm> connectionTabs;
    private JPanel mainPanel;

    TabbedBrowserForm(@NotNull BrowserToolWindowForm parent) {
        super(parent);
        connectionTabs = new DBNTabbedPane<>(this);
        connectionTabs.setAutoscrolls(true);
        connectionTabs.enableFocusInheritance();
        //mainPanel.add(connectionTabs, BorderLayout.CENTER);
        initBrowserForms();
        ProjectEvents.subscribe(ensureProject(), this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
        connectionTabs.addTabSelectionListener(i ->
                ProjectEvents.notify(ensureProject(),
                        BrowserTreeEventListener.TOPIC,
                        (listener) -> listener.selectionChanged()));
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                EnvironmentSettings environmentSettings = getEnvironmentSettings(project);
                EnvironmentVisibilitySettings visibilitySettings = environmentSettings.getVisibilitySettings();
                for (Component component : getTabComponents()) {
                    guarded(component, c -> updateTabColor(c, visibilitySettings));
                }
            }
        };
    }

    private void updateTabColor(Component component, EnvironmentVisibilitySettings visibilitySettings) {
        SimpleBrowserForm browserForm = TAB_CONTENT.get(component);
        ConnectionHandler connection = browserForm.getConnection();
        if (connection == null) return;

        if (visibilitySettings.getConnectionTabs().value()) {
            Color environmentColor = connection.getEnvironmentType().getColor();
            connectionTabs.setTabColor(component, environmentColor);
        } else {
            connectionTabs.setTabColor(component, null);
        }
    }


    private void initBrowserForms() {
        JPanel mainPanel = this.mainPanel;
        if (mainPanel == null) return;

        Project project = ensureProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        for (ConnectionHandler connection: connectionBundle.getConnections()) {
            SimpleBrowserForm browserForm = new SimpleBrowserForm(this, connection);

            JComponent component = browserForm.getComponent();
            String title = Commons.nvl(connection.getName(), txt("app.connection.placeholder.UnnamedConnection"));
            Icon icon = null; //connection.getIcon();

            EnvironmentType environmentType = connection.getEnvironmentType();
            Color color = environmentType.getColor();

            initTabComponent(component, icon, color, browserForm);
            this.connectionTabs.addTab(title, component);
        }
        if (this.connectionTabs.getTabCount() == 0) {
            mainPanel.removeAll();
            mainPanel.add(new JBList(new ArrayList()), BorderLayout.CENTER);
        } else {
            if (mainPanel.getComponentCount() > 0) {
                Component component = mainPanel.getComponent(0);
                if (component != this.connectionTabs) {
                    mainPanel.removeAll();
                    mainPanel.add(this.connectionTabs, BorderLayout.CENTER);
                }
            } else {
                mainPanel.add(this.connectionTabs, BorderLayout.CENTER);
            }
        }
    }

    @Nullable
    private SimpleBrowserForm getBrowserForm(ConnectionId connectionId) {
        var connectionTabs = getConnectionTabs();
        for (Component component : connectionTabs.getTabbedComponents()) {
            SimpleBrowserForm browserForm = TAB_CONTENT.get(component);
            ConnectionHandler connection = browserForm.getConnection();
            if (connection != null && connection.getConnectionId() == connectionId) {
                return browserForm;
            }
        }
        return null;
    }

    @Nullable
    private SimpleBrowserForm removeBrowserForm(ConnectionId connectionId) {
        var connectionTabs = getConnectionTabs();
        for (Component component : connectionTabs.getTabbedComponents()) {
            SimpleBrowserForm browserForm = TAB_CONTENT.get(component);
            ConnectionId tabConnectionId = browserForm.getConnectionId();
            if (tabConnectionId == connectionId) {
                connectionTabs.removeTab(component, false);
                return browserForm;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    @Nullable
    public DatabaseBrowserTree getBrowserTree() {
        return getActiveBrowserTree();
    }

    @Nullable
    public DatabaseBrowserTree getBrowserTree(ConnectionId connectionId) {
        SimpleBrowserForm browserForm = getBrowserForm(connectionId);
        return browserForm == null ? null : browserForm.getBrowserTree();
    }

    @Nullable
    private SimpleBrowserForm getSelectedBrowserForm() {
        Component component = getSelectedTabComponent();
        return ClientProperty.TAB_CONTENT.get(component);
    }

    private Component getSelectedTabComponent() {
        return getConnectionTabs().getSelectedTabComponent();
    }

    @Nullable
    public DatabaseBrowserTree getActiveBrowserTree() {
        SimpleBrowserForm browserForm = getSelectedBrowserForm();
        return browserForm == null ? null : browserForm.getBrowserTree();
    }

    @Override
    public ConnectionId getSelectedConnection() {
        SimpleBrowserForm browserForm = getSelectedBrowserForm();
        return browserForm == null ? null : browserForm.getConnectionId();
    }

    @Override
    public void selectConnection(ConnectionId connectionId) {
        SimpleBrowserForm browserForm = getBrowserForm(connectionId);
        if (browserForm == null) return;
        getConnectionTabs().selectTab(browserForm);
    }

    @Override
    public void selectElement(BrowserTreeNode treeNode, boolean focus, boolean scroll) {
        ConnectionId connectionId = treeNode.getConnectionId();
        SimpleBrowserForm browserForm = getBrowserForm(connectionId);
        if (browserForm == null) return;

        if (scroll) browserForm.selectElement(treeNode, focus, true);

        selectConnection(connectionId);
    }

    @Override
    public void rebuildTree() {
        getTabComponents()
            .stream()
            .map(c -> (SimpleBrowserForm) TAB_CONTENT.get(c))
            .forEach(f -> f.rebuildTree());
    }

    @NotNull
    public DBNTabbedPane<SimpleBrowserForm> getConnectionTabs() {
        return Failsafe.nn(connectionTabs);
    }

    void refreshTabInfo(ConnectionId connectionId) {
        for (Component component : getTabComponents()) {
            SimpleBrowserForm browserForm = TAB_CONTENT.get(component);
            ConnectionHandler connection = browserForm.getConnection();
            if (connection == null) continue;

            if (connection.getConnectionId() == connectionId) {
                String title = connection.getName();
                connectionTabs.setTabTitle(component, title);
                break;
            }
        }

    }

    private List<Component> getTabComponents() {
        return getConnectionTabs().getTabbedComponents();
    }
}

