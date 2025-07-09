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

package com.dbn.connection.config.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.common.ui.dialog.DialogNotificationListener;
import com.dbn.common.ui.dialog.DialogNotificationPanel;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Messages;
import com.dbn.common.util.NotificationStatus;
import com.dbn.common.util.Safe;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectivityStatus;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionDebuggerSettings;
import com.dbn.connection.config.ConnectionDetailSettings;
import com.dbn.connection.config.ConnectionFilterSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.options.ConfigActivity.CLONING;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ConnectionSettingsForm extends CompositeConfigurationEditorForm<ConnectionSettings> {
    public static final String OCI_BIND_PORT_WARNING = "OCI_BIND_PORT_WARNING";
    public static final String OCI_OFFER_USER_TOOLKIT_PLUGIN = "OCI_OFFER_USER_TOOLKIT_PLUGIN";
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel headerPanel;
    private JButton infoButton;
    private JButton testButton;

    private DBNTabbedPane tabbedPane;
    private DBNHeaderForm headerForm;
    private DialogNotificationPanel notificationPanel;

    public ConnectionSettingsForm(ConnectionSettings connectionSettings) {
        super(connectionSettings);

        initConfigTabs(connectionSettings);
        initHeaderPanel(connectionSettings);

        resetFormChanges();

        registerComponent(testButton);
        registerComponent(infoButton);
        ProjectEvents.subscribe(ensureProject(), this, ConnectionPresentationChangeListener.TOPIC, connectionPresentationChangeListener);
    }

    private void initConfigTabs(ConnectionSettings connectionSettings) {
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        tabbedPane = new DBNTabbedPane(this);
        tabbedPane.setTabComponentInsets(DBNTabbedPane.REGULAR_INSETS);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addTab(txt("cfg.connection.title.Database"), databaseSettings.createComponent());

        if (databaseSettings.getConfigType() == ConnectionConfigType.BASIC) {
            // TODO enable when ssl connectivity is implemented
            //ConnectionSslSettings sslSettings = connectionSettings.getSslSettings();
            //tabbedPane.addTab(txt("cfg.connection.title.Ssl"), new JBScrollPane(sslSettings.createComponent()));

            ConnectionSshTunnelSettings sshTunnelSettings = connectionSettings.getSshTunnelSettings();
            tabbedPane.addTab(txt("cfg.connection.title.SshTunnel"), new JBScrollPane(sshTunnelSettings.createComponent()));
        }

        ConnectionPropertiesSettings propertiesSettings = connectionSettings.getPropertiesSettings();
        tabbedPane.addTab(txt("cfg.connection.title.Properties"), new JBScrollPane(propertiesSettings.createComponent()));

        ConnectionDetailSettings detailSettings = connectionSettings.getDetailSettings();
        tabbedPane.addTab(txt("cfg.connection.title.Details"), new JBScrollPane(detailSettings.createComponent()));

        if (databaseSettings.getDatabaseType() == DatabaseType.ORACLE) {
            ConnectionDebuggerSettings debuggerSettings = connectionSettings.getDebuggerSettings();
            tabbedPane.addTab(txt("cfg.connection.title.Debugger"), new JBScrollPane(debuggerSettings.createComponent()));
        }

        ConnectionFilterSettings filterSettings = connectionSettings.getFilterSettings();
        tabbedPane.addTab(txt("cfg.connection.title.Filters"), new JBScrollPane(filterSettings.createComponent()));
    }

    private void initHeaderPanel(ConnectionSettings connectionSettings) {
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        ConnectionDetailSettings detailSettings = connectionSettings.getDetailSettings();
        ConnectivityStatus connectivityStatus = databaseSettings.getConnectivityStatus();
        Icon icon = connectionSettings.isNew() ? Icons.CONNECTION_NEW :
                   !connectionSettings.isActive() ? Icons.CONNECTION_DISABLED :
                   connectivityStatus == ConnectivityStatus.VALID ? Icons.CONNECTION_CONNECTED :
                   connectivityStatus == ConnectivityStatus.INVALID ? Icons.CONNECTION_INVALID : Icons.CONNECTION_INACTIVE;

        String name = connectionSettings.getDatabaseSettings().getName();
        Color color = detailSettings.getEnvironmentType().getColor();

        headerForm = new DBNHeaderForm(this, name, icon, color);
        testButton = new JButton(txt("cfg.connection.button.TestConnection"));
        infoButton = new JButton(txt("cfg.connection.button.Info"));
        headerForm.addButton(testButton);
        headerForm.addButton(infoButton);

        DialogNotificationPanel.Builder panelBuilder = new DialogNotificationPanel.Builder();
        panelBuilder.project(ensureProject());
        panelBuilder.backgroundColor(color);
        // use default error label
        panelBuilder.addComponent(OCI_BIND_PORT_WARNING);
        panelBuilder.addComponent(OCI_OFFER_USER_TOOLKIT_PLUGIN);

        ///setIcon(Icons.COMMON_INFO);
//        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
//                "You may wish to install OCI support for this database.",
//                "Oracle OCI Toolkit Plugin",
//                "https://plugins.jetbrains.com/plugin/22952-oracle-oci-toolkit");

        this.notificationPanel = panelBuilder.build();
        notificationPanel.init();
        notificationPanel.setVisible(false);

        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        headerPanel.add(notificationPanel, BorderLayout.PAGE_END);
    }

    public ConnectionSettings getTemporaryConfig() throws ConfigurationException {
        try {
            ConfigMonitor.set(CLONING, true);

            UserInterface.stopTableCellEditing(mainPanel);
            ConnectionSettings configuration = getConfiguration();
            ConnectionSettings clone = configuration.clone();
            clone.getDatabaseSettings().getAuthenticationInfo().setTemporary(true);

            ConnectionDatabaseSettingsForm databaseSettingsEditor = configuration.getDatabaseSettings().getSettingsEditor();
            if(databaseSettingsEditor != null) databaseSettingsEditor.applyFormChanges(clone.getDatabaseSettings());

            ConnectionPropertiesSettingsForm propertiesSettingsEditor = configuration.getPropertiesSettings().getSettingsEditor();
            if (propertiesSettingsEditor != null) propertiesSettingsEditor.applyFormChanges(clone.getPropertiesSettings());

            ConnectionSshTunnelSettingsForm sshTunnelSettingsForm = configuration.getSshTunnelSettings().getSettingsEditor();
            if (sshTunnelSettingsForm != null) sshTunnelSettingsForm.applyFormChanges(clone.getSshTunnelSettings());

            ConnectionSslSettingsForm sslSettingsForm = configuration.getSslSettings().getSettingsEditor();
            if (sslSettingsForm != null) sslSettingsForm.applyFormChanges(clone.getSslSettings());

            ConnectionDetailSettingsForm detailSettingsForm = configuration.getDetailSettings().getSettingsEditor();
            if (detailSettingsForm != null) detailSettingsForm.applyFormChanges(clone.getDetailSettings());

            ConnectionDebuggerSettingsForm debuggerSettingsForm = configuration.getDebuggerSettings().getSettingsEditor();
            if (debuggerSettingsForm != null) debuggerSettingsForm.applyFormChanges(clone.getDebuggerSettings());

            ConnectionFilterSettingsForm filterSettingsForm = configuration.getFilterSettings().getSettingsEditor();
            if (filterSettingsForm != null) filterSettingsForm.applyFormChanges(clone.getFilterSettings());

            return clone;
        } finally {
            ConfigMonitor.set(CLONING, false);
        }
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            Object source = e.getSource();
            ConnectionSettings configuration = getConfiguration();
            if (source == testButton || source == infoButton) {
                ConnectionSettingsForm connectionSettingsForm = configuration.getSettingsEditor();
                if (connectionSettingsForm == null) return;

                Project project = ensureProject();
                try {
                    ConnectionSettings temporaryConfig = connectionSettingsForm.getTemporaryConfig();
                    ConnectionManager connectionManager = ConnectionManager.getInstance(project);

                    if (source == testButton) {
                        connectionManager.testConfigConnection(temporaryConfig, true);
                    } else if (source == infoButton) {
                        ConnectionDetailSettingsForm detailSettingsForm = configuration.getDetailSettings().getSettingsEditor();
                        if (detailSettingsForm != null) {
                            EnvironmentType environmentType = detailSettingsForm.getSelectedEnvironmentType();
                            connectionManager.showConnectionInfo(temporaryConfig, environmentType);
                        }
                    }
                    configuration.getDatabaseSettings().setConnectivityStatus(temporaryConfig.getDatabaseSettings().getConnectivityStatus());
                    refreshConnectionList(configuration);
                } catch (ConfigurationException e1) {
                    conditionallyLog(e1);
                    Messages.showErrorDialog(project, txt("cfg.connection.title.InvalidConfiguration"), e1.getMessage());
                }
            }
        };
    }

    protected void refreshConnectionList(ConnectionSettings configuration) {
        ConnectionBundleSettings bundleSettings = configuration.ensureParent();
        ConnectionBundleSettingsForm bundleSettingsEditor = bundleSettings.getSettingsEditor();
        if (bundleSettingsEditor == null) return;

        JList connectionList = bundleSettingsEditor.getList();
        UserInterface.repaint(connectionList);
        ConnectionDatabaseSettingsForm settingsEditor = configuration.getDatabaseSettings().getSettingsEditor();
        if (settingsEditor == null) return;

        settingsEditor.notifyPresentationChanges();
    }

    public void deselectTab() {
        Safe.run(notificationPanel, n -> n.disableNotifications());
    }
    public void selectTab(String tabName) {
        Safe.run(tabbedPane, t -> t.selectTab(tabName));
        Safe.run(this, n -> n.updateForSelected());
    }

    private void updateForSelected() {
        notificationPanel.enableNotifications();
        checkToolkitNotifications();
    }

    private void checkToolkitNotifications() {
        // TODO: reenable when have plugin detection working.
        boolean hasOfferToolkitEvent = true; //notificationPanel.searchForEvents(OCI_OFFER_USER_TOOLKIT_PLUGIN);
        // if no event, check if we need to add one
        if (!hasOfferToolkitEvent) {
            NotificationStatus status = new NotificationStatus(NotificationStatus.Severity.INFO,
                    "It appears you don't have OCI Toolkit...");
            // for now just assume
            final DialogNotificationListener.NotificationStatusEvent event =
                    new DialogNotificationListener.NotificationStatusEvent(
                            this, ConnectionSettingsForm.OCI_OFFER_USER_TOOLKIT_PLUGIN, status);
            ProjectEvents.notify(ensureProject(), DialogNotificationListener.TOPIC,
                    notificationListener -> {
                        notificationListener.fireNotificatonStatusEvent(event);
                    });
        }
    }

    public String getSelectedTabName() {
        return Safe.call(tabbedPane, t -> t.getSelectedTabTitle());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private final ConnectionPresentationChangeListener connectionPresentationChangeListener = new ConnectionPresentationChangeListener() {
        @Override
        public void presentationChanged(String name, Icon icon, Color color, ConnectionId connectionId, DatabaseType databaseType) {
            dispatch(() -> {
                if (isNotValid(ConnectionSettingsForm.this)) return;

                ConnectionSettings configuration = getConfiguration();
                if (!configuration.getConnectionId().equals(connectionId)) return;

                DBNHeaderForm header = headerForm;
                if (header == null) return;

                if (name != null) header.setTitle(name);
                if (icon != null) header.setIcon(icon);
                if (color != null) header.setBackground(color); else header.setBackground(Colors.getPanelBackground());
                //if (databaseType != null) databaseIconLabel.setIcon(databaseType.getLargeIcon());
            });
        }
    };

    @Override
    public void resetFormChanges() {
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        UserInterface.stopTableCellEditing(mainPanel);
    }

    @Override
    public void applyFormChanges(ConnectionSettings configuration) throws ConfigurationException {
    }
}
