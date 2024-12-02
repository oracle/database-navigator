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
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Safe;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectivityStatus;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionDebuggerSettings;
import com.dbn.connection.config.ConnectionDetailSettings;
import com.dbn.connection.config.ConnectionFilterSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;
import com.dbn.connection.config.ConnectionSslSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.options.ConfigActivity.CLONING;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ConnectionSettingsForm extends CompositeConfigurationEditorForm<ConnectionSettings> {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel headerPanel;
    private JButton infoButton;
    private JButton testButton;
    private JCheckBox activeCheckBox;

    private final DBNTabbedPane tabbedPane;
    private final DBNHeaderForm headerForm;

    public ConnectionSettingsForm(ConnectionSettings connectionSettings) {
        super(connectionSettings);
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        tabbedPane = new DBNTabbedPane(this);
        tabbedPane.setTabComponentInsets(DBNTabbedPane.REGULAR_INSETS);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addTab(txt("cfg.connection.title.Database"), databaseSettings.createComponent());

        if (databaseSettings.getConfigType() == ConnectionConfigType.BASIC) {
            ConnectionSslSettings sslSettings = connectionSettings.getSslSettings();
            tabbedPane.addTab(txt("cfg.connection.title.Ssl"), new JBScrollPane(sslSettings.createComponent()));

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

        ConnectivityStatus connectivityStatus = databaseSettings.getConnectivityStatus();
        Icon icon = connectionSettings.isNew() ? Icons.CONNECTION_NEW :
                   !connectionSettings.isActive() ? Icons.CONNECTION_DISABLED :
                   connectivityStatus == ConnectivityStatus.VALID ? Icons.CONNECTION_CONNECTED :
                   connectivityStatus == ConnectivityStatus.INVALID ? Icons.CONNECTION_INVALID : Icons.CONNECTION_INACTIVE;

        String name = connectionSettings.getDatabaseSettings().getName();
        Color color = detailSettings.getEnvironmentType().getColor();

        headerForm = new DBNHeaderForm(this, name, icon, color);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        ProjectEvents.subscribe(ensureProject(), this, ConnectionPresentationChangeListener.TOPIC, connectionPresentationChangeListener);

        //databaseSettingsForm.notifyPresentationChanges();
        //detailSettingsForm.notifyPresentationChanges();

        resetFormChanges();

        registerComponent(testButton);
        registerComponent(infoButton);
        registerComponent(activeCheckBox);
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
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                ConnectionSettings configuration = getConfiguration();
                if (source == testButton || source == infoButton) {
                    ConnectionSettingsForm connectionSettingsForm = configuration.getSettingsEditor();
                    if (connectionSettingsForm != null) {
                        Project project = ensureProject();
                        try {
                            ConnectionSettings temporaryConfig = connectionSettingsForm.getTemporaryConfig();
                            ConnectionManager connectionManager = ConnectionManager.getInstance(project);

                            if (source == testButton) connectionManager.testConfigConnection(temporaryConfig, true);
                            if (source == infoButton) {
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
                            Messages.showErrorDialog(project, "Configuration error", e1.getMessage());
                        }
                    }
                }
                if (source == activeCheckBox) {
                    configuration.setModified(true);
                    refreshConnectionList(configuration);
                }

            }

            private void refreshConnectionList(ConnectionSettings configuration) {
                ConnectionBundleSettings bundleSettings = configuration.ensureParent();
                ConnectionBundleSettingsForm bundleSettingsEditor = bundleSettings.getSettingsEditor();
                if (bundleSettingsEditor == null) return;

                JList connectionList = bundleSettingsEditor.getList();
                UserInterface.repaint(connectionList);
                ConnectionDatabaseSettingsForm settingsEditor = configuration.getDatabaseSettings().getSettingsEditor();
                if (settingsEditor == null) return;

                settingsEditor.notifyPresentationChanges();
            }
        };
    }

    public boolean isConnectionActive() {
        return activeCheckBox.isSelected();
    }

    public void selectTab(String tabName) {
        Safe.run(tabbedPane, t -> t.selectTab(tabName));
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
            Dispatch.run(() -> {
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
        activeCheckBox.setSelected(getConfiguration().isActive());
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        UserInterface.stopTableCellEditing(mainPanel);
        applyFormChanges(getConfiguration());
    }

    @Override
    public void applyFormChanges(ConnectionSettings configuration) throws ConfigurationException {
        boolean settingsChanged = configuration.isActive() != activeCheckBox.isSelected();
        configuration.setActive(activeCheckBox.isSelected());

        SettingsChangeNotifier.register(() -> {
            if (settingsChanged) {
                ProjectEvents.notify(getProject(),
                        ConnectionConfigListener.TOPIC,
                        listener -> listener.connectionChanged(configuration.getConnectionId()));
            }
        });
    }
}
