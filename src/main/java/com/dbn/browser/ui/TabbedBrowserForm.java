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
import com.dbn.common.ui.tab.DBNColoredTabs;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.ui.util.ClientProperty.FORM;

public class TabbedBrowserForm extends DatabaseBrowserForm{
    private final DBNColoredTabs<SimpleBrowserForm> connectionTabs;
    private JPanel mainPanel;

    TabbedBrowserForm(@NotNull BrowserToolWindowForm parent) {
        super(parent);
        connectionTabs = new DBNColoredTabs<>(this);
        mainPanel.add(connectionTabs);

        initBrowserForms();
        ProjectEvents.subscribe(ensureProject(), this, EnvironmentManagerListener.TOPIC, environmentManagerListener());

        connectionTabs.onTabSelected(i -> ProjectEvents.notify(ensureProject(),
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
        SimpleBrowserForm browserForm = FORM.get(component);
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

            this.connectionTabs.addTab(title, component);
            this.connectionTabs.setTabColor(component, color);
        }
    }

    @Nullable
    private SimpleBrowserForm getBrowserForm(ConnectionId connectionId) {
        var connectionTabs = getConnectionTabs();
        for (Component component : connectionTabs.getTabbedComponents()) {
            SimpleBrowserForm browserForm = FORM.get(component);
            ConnectionHandler connection = browserForm.getConnection();
            if (connection != null && connection.getConnectionId() == connectionId) {
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
        return ClientProperty.FORM.get(component);
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

        getConnectionTabs().selectTab(browserForm, false);
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
            .map(c -> (SimpleBrowserForm) FORM.get(c))
            .forEach(f -> f.rebuildTree());
    }

    @NotNull
    public DBNColoredTabs<SimpleBrowserForm> getConnectionTabs() {
        return Failsafe.nn(connectionTabs);
    }

    void refreshTabInfo(ConnectionId connectionId) {
        for (Component component : getTabComponents()) {
            SimpleBrowserForm browserForm = FORM.get(component);
            ConnectionHandler connection = browserForm.getConnection();
            if (connection == null) continue;

            if (connection.getConnectionId() == connectionId) {
                String title = connection.getName();
                connectionTabs.setTabTitle(component, title);
                break;
            }
        }

    }

    private List<JComponent> getTabComponents() {
        return getConnectionTabs().getTabbedComponents();
    }
}

