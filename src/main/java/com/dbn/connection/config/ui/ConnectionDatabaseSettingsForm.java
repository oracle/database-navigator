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

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Commons;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectivityStatus;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.file.DatabaseFileBundle;
import com.dbn.connection.config.provider.CloudConfigProviderAuthentication;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigSourceType;
import com.dbn.credentials.Secret;
import com.dbn.driver.DriverSource;
import com.dbn.oci.config.OciAuthenticationConfig;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.Objects;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.connection.AuthenticationType.NONE;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.nls.NlsResources.txt;
import static java.awt.event.KeyEvent.VK_UNDEFINED;

@SuppressWarnings("unused")
public class ConnectionDatabaseSettingsForm extends ConfigurationEditorForm<ConnectionDatabaseSettings> {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JTextField descriptionTextField;
    private JComboBox<DatabaseType> databaseTypeComboBox;
    private JPanel driverLibraryPanel;
    private JLabel databaseTypeLabel;
    private JPanel authenticationPanel;
    private JPanel urlPanel;
    private JPanel databaseTypeHintPanel;

    private final ConnectionUrlSettingsForm urlSettingsForm;
    private final ConnectionDriverSettingsForm driverSettingsForm;
    private final ConnectionAuthenticationSettingsForm authSettingsForm;

    private DatabaseType selectedDatabaseType;

    public ConnectionDatabaseSettingsForm(ConnectionDatabaseSettings configuration) {
        super(configuration);

        ConnectionConfigType configType = configuration.getConfigType();

        selectedDatabaseType = configuration.getDatabaseType();
        if (configType == ConnectionConfigType.CUSTOM) {
            initComboBox(databaseTypeComboBox,
                    DatabaseType.ORACLE,
                    DatabaseType.MYSQL,
                    DatabaseType.POSTGRES,
                    DatabaseType.SQLITE,
                    DatabaseType.GENERIC);
        } else {
            databaseTypeLabel.setText(selectedDatabaseType.getName());
            databaseTypeLabel.setIcon(selectedDatabaseType.getIcon());
            databaseTypeLabel.setDisplayedMnemonic(VK_UNDEFINED);
            initComboBox(databaseTypeComboBox, selectedDatabaseType);
            setSelection(databaseTypeComboBox, selectedDatabaseType);
            databaseTypeComboBox.setEnabled(false);
            databaseTypeComboBox.setVisible(false);
        }

        urlSettingsForm = new ConnectionUrlSettingsForm(this, configuration);
        authSettingsForm = new ConnectionAuthenticationSettingsForm(this);
        driverSettingsForm = new ConnectionDriverSettingsForm(this);
		boolean externalLibrary = configuration.getDriverSource() == DriverSource.EXTERNAL;

        urlPanel.add(urlSettingsForm.getComponent(), BorderLayout.CENTER);
        authenticationPanel.add(authSettingsForm.getComponent(), BorderLayout.CENTER);
        driverLibraryPanel.add(driverSettingsForm.getComponent(), BorderLayout.CENTER);
        authSettingsForm.addCloudProviderChangeListeners(urlSettingsForm::updateUrlField);

        resetFormChanges();
        registerComponent(mainPanel);

        DatabaseType databaseType = configuration.getDatabaseType();
        AuthenticationType[] authTypes = databaseType.getAuthTypes();

        urlSettingsForm.updateFieldVisibility();
        updateAuthenticationVisibility();


        if (configType == ConnectionConfigType.CUSTOM) {
            databaseTypeComboBox.addActionListener(e -> databaseTypeChanged());
            driverSettingsForm.getDriverComboBox().addActionListener(e -> updateNativeSupportDatabaseHint());
            updateNativeSupportDatabaseHint();
        }
    }

    protected void databaseTypeChanged() {
        DatabaseType oldDatabaseType = selectedDatabaseType;
        DatabaseType newDatabaseType = getSelection(databaseTypeComboBox);
        selectedDatabaseType = newDatabaseType;

        AuthenticationType[] authTypes = newDatabaseType.getAuthTypes();

        urlSettingsForm.handleDatabaseTypeChange(oldDatabaseType, newDatabaseType);
        updateAuthenticationVisibility();
        driverSettingsForm.updateDriverFields();

        updateNativeSupportDatabaseHint();
    }

    void notifyPresentationChanges() {
        ConnectionDatabaseSettings configuration = getConfiguration();
        String name = getConnectionName();
        ConnectivityStatus connectivityStatus = configuration.getConnectivityStatus();
        ConnectionSettings connectionSettings = configuration.ensureParent();
        Icon icon = getIcon(connectionSettings, connectivityStatus);

        EnvironmentType environmentType = connectionSettings.getDetailSettings().getEnvironmentType();
        Color color = environmentType.getColor();
        ConnectionId connectionId = configuration.getConnectionId();
        DatabaseType databaseType = configuration.getDatabaseType();

        ProjectEvents.notify(
                configuration.getProject(),
                ConnectionPresentationChangeListener.TOPIC,
                (listener) -> listener.presentationChanged(name, icon, color, connectionId, databaseType));
    }

    OciAuthenticationConfig getOciAuthenticationConfig() {
        return authSettingsForm.getOciAuthenticationConfig();
    }

    private Icon getIcon(ConnectionSettings connectionSettings, ConnectivityStatus connectivityStatus) {
        ConnectionSettingsForm settingsEditor = connectionSettings.getSettingsEditor();

        return connectionSettings.isNew() ? Icons.CONNECTION_NEW :
                settingsEditor != null && !connectionSettings.isActive() ? Icons.CONNECTION_DISABLED :
                        connectivityStatus == ConnectivityStatus.VALID ? Icons.CONNECTION_CONNECTED :
                        connectivityStatus == ConnectivityStatus.INVALID ? Icons.CONNECTION_INVALID : Icons.CONNECTION_INACTIVE;
    }

    //protected abstract ConnectionDatabaseSettings createConfig(ConnectionSettings configuration);

    @Override
    protected DocumentListener createDocumentListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                mackConfigModified();

                Document document = e.getDocument();

                if (document == driverSettingsForm.getDriverLibraryTextField().getTextField().getDocument()) {
                    driverSettingsForm.updateDriverFields();
                }

                if (document == nameTextField.getDocument()) {
                    ConnectionBundleSettings connectionBundleSettings = getConnectionBundleSettings();
                    ConnectionBundleSettingsForm settingsEditor = connectionBundleSettings.getSettingsEditor();
                    if (settingsEditor != null) {
                        JList<?> connectionList = settingsEditor.getList();
                        UserInterface.repaint(connectionList);
                        notifyPresentationChanges();
                    }
                }
            }
        };
    }


    @Override
    protected ActionListener createActionListener() {
        return e -> {
            Object source = e.getSource();
            mackConfigModified();
            if (source == nameTextField) {
                ConnectionBundleSettings connectionBundleSettings = getConnectionBundleSettings();
                ConnectionBundleSettingsForm settingsEditor = connectionBundleSettings.getSettingsEditor();

                if (settingsEditor != null) {
                    JList<?> connectionList = settingsEditor.getList();
                    UserInterface.repaint(connectionList);
                    notifyPresentationChanges();
                }
            }
        };
    }

    private @NotNull ConnectionBundleSettings getConnectionBundleSettings() {
        return getConfiguration().ensureParent().ensureParent();
    }

    public String getConnectionName() {
        return getText(nameTextField);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges(final ConnectionDatabaseSettings configuration) throws ConfigurationException {
        if (isCloudProviderAuthenticationVisible()) {
            authSettingsForm.validateCloudProviderSettings();
        }

        DatabaseType databaseType = getSelectedDatabaseType();
        DriverOption driverOption = driverSettingsForm.getDriverOption();
        DatabaseUrlType urlType = Commons.nvl(urlSettingsForm.getUrlType(), DatabaseUrlType.CUSTOM);

        boolean localConfigFile = urlType == DatabaseUrlType.PROVIDER &&
                urlSettingsForm.getConfigSourceType() == ConfigSourceType.FILE;

        String url = urlSettingsForm.getUrl();
        DatabaseUrlPattern urlPattern = urlType == DatabaseUrlType.CUSTOM ?
                Commons.nvl(databaseType.resolveUrlPattern(url), DatabaseUrlPattern.GENERIC) :
                DatabaseUrlPattern.get(databaseType, urlType);

        configuration.setDatabaseType(databaseType);
        configuration.setName(getConnectionName());
        configuration.setDescription(getText(descriptionTextField));
        configuration.setDriverLibrary(driverSettingsForm.getDriverLibrary());
        configuration.setDriver(driverOption == null ? null : driverOption.getName());
        configuration.setUrlPattern(urlPattern);

        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        ConfigProviderInfo configProviderInfo = configuration.getConfigProviderInfo();
        Secret[] oldConfigProviderSecrets = configProviderInfo.snapshotSecrets();
        databaseInfo.reset();

        databaseInfo.setUrlType(urlType);
        databaseInfo.setUrl(url);

        if (urlType == DatabaseUrlType.CUSTOM) {
            databaseInfo.initializeDetails(urlPattern);
        } else if (urlType == DatabaseUrlType.EZCONNECT) {
            databaseInfo.setServerType(urlSettingsForm.getServerType());
            databaseInfo.setHost(urlSettingsForm.getHost());
            databaseInfo.setPort(urlSettingsForm.getPort());
            databaseInfo.setDatabase(urlSettingsForm.getDatabase());
            databaseInfo.setProtocol(urlSettingsForm.getProtocol());
            databaseInfo.setServerType(urlSettingsForm.getServerType());
            databaseInfo.setParameters(urlSettingsForm.getParameters());
        }
        else if (urlType == DatabaseUrlType.TNS) {
        	databaseInfo.setTnsFolder(urlSettingsForm.getTnsFolder());
        	databaseInfo.setTnsProfile(urlSettingsForm.getTnsProfile());
        } else if (urlType == DatabaseUrlType.PROVIDER) {
            urlSettingsForm.applyConfigProviderInfo(configuration.getConfigProviderInfo());
            if (isCloudProviderAuthenticationVisible()) {
                authSettingsForm.applyCloudProviderFormChanges(configuration.getConfigProviderInfo());
            }
            if (urlSettingsForm.getConfigSourceType() == ConfigSourceType.FILE &&
                    isEmptyOrSpaces(urlSettingsForm.getConfigLocation())) {
                throw new ConfigurationException("Config file is required.");
            }
        } else if (urlType == DatabaseUrlType.FILE){
            DatabaseFileBundle fileBundle = urlSettingsForm.getFileBundle();
            fileBundle.validate();
            databaseInfo.setFileBundle(fileBundle);
        } else {
            databaseInfo.setHost(urlSettingsForm.getHost());
            databaseInfo.setPort(urlSettingsForm.getPort());
            databaseInfo.setDatabase(urlSettingsForm.getDatabase());
        }

        // create snapshot of earlier authentication
        AuthenticationInfo authenticationInfo = configuration.getAuthenticationInfo();
        Secret[] oldSecrets = authenticationInfo.snapshotSecrets();

        // apply changes and create snapshot of new authentication
        if (urlSettingsForm.isCloudProviderConfig()) {
            authenticationInfo.setType(AuthenticationType.NONE);
        } else {
            authSettingsForm.applyFormChanges(authenticationInfo);
        }

        if (localConfigFile) {
            authenticationInfo.setType(AuthenticationType.NONE);
        }
        //Secret[] newSecrets = authenticationInfo.getSecrets();

        if (!authenticationInfo.isTemporary()) {
            // update password store if authentication info is not marked as temporary
            authenticationInfo.updateSecrets(oldSecrets);
        }
        if (!ConfigMonitor.isCloning()) {
            configProviderInfo.updateSecrets(oldConfigProviderSecrets);
        }

        configuration.setDriverSource(driverSettingsForm.getDriverSource());
        configuration.updateSignature();
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConfigurationEditors.validateStringValue(nameTextField, txt("cfg.connection.field.Name"), true);
        ConnectionDatabaseSettings configuration = getConfiguration();

        DatabaseType selectedDatabaseType = getSelectedDatabaseType();
        DatabaseType driverDatabaseType = driverSettingsForm.getDriverDatabaseType();
        if (driverDatabaseType != null && driverDatabaseType != selectedDatabaseType) {
            if (selectedDatabaseType == DatabaseType.GENERIC) {
                // TODO hint there is dedicated support for the database type resolved from driver
            } else {
                throw new ConfigurationException(txt("cfg.connection.error.InvalidDriverLibraryType", selectedDatabaseType.getName()));
            }
        }

        String oldName = configuration.getName();
        String newName = getText(nameTextField);
        boolean nameChanged = !Objects.equals(newName, oldName);

        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        boolean settingsChanged =
                urlSettingsForm.settingsChanged() ||
                (isCloudProviderAuthenticationVisible() ?
                        authSettingsForm.cloudProviderSettingsChanged() :
                        authSettingsForm.settingsChanged()) ||
                !Commons.match(configuration.getDatabaseType(), selectedDatabaseType) ||
                !Commons.match(configuration.getDriverLibrary(), driverSettingsForm.getDriverLibrary());

        applyFormChanges(configuration);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            ConnectionId connectionId = configuration.getConnectionId();
            if (nameChanged) {
                ProjectEvents.notify(project,
                        ConnectionConfigListener.TOPIC,
                        listener -> listener.connectionNameChanged(connectionId, oldName));
            }

            if (settingsChanged) {
                ProjectEvents.notify(project,
                        ConnectionConfigListener.TOPIC,
                        listener -> listener.connectionChanged(connectionId));
            }
        });
    }

    @NotNull
    DatabaseType getSelectedDatabaseType() {
        ConnectionDatabaseSettings configuration = getConfiguration();
        return Commons.nvl(getSelection(databaseTypeComboBox), configuration.getDatabaseType());
    }

    void updateAuthenticationVisibility() {
        DatabaseType databaseType = getSelectedDatabaseType();
        authSettingsForm.setAuthenticationTypes(getAuthenticationTypes());
        authSettingsForm.setCredentialsTitle(getCredentialsTitle());
        boolean cloudProviderConfig = urlSettingsForm.isCloudProviderConfig();
        CloudConfigProviderType cloudProviderType = isCloudProviderAuthenticationVisible() ?
                urlSettingsForm.getCloudConfigProviderType() :
                null;
        authSettingsForm.setCloudProviderMode(cloudProviderType);
        authenticationPanel.setVisible(cloudProviderType != null ||
                !cloudProviderConfig && databaseType.supportsAuthentication() && urlSettingsForm.requiresAuthentication());
        if (driverSettingsForm != null) {
            driverSettingsForm.updateDriverFields();
        }
    }

    CloudConfigProviderType getExternalLibraryCloudProvider() {
        CloudConfigProviderType provider = urlSettingsForm.getCloudConfigProviderType();
        if (getSelectedDatabaseType() != DatabaseType.ORACLE) return null;
        if (!urlSettingsForm.isCloudProviderConfig()) return null;
        if (provider == null) return null;

        return provider;
    }

    DatabaseUrlType getUrlType() {
        return urlSettingsForm.getUrlType();
    }

    CloudConfigProviderType getCloudConfigProviderType() {
        return urlSettingsForm.isCloudProviderConfig() ? urlSettingsForm.getCloudConfigProviderType() : null;
    }

    void addJsonExportChangeListeners(Runnable listener) {
        databaseTypeComboBox.addActionListener(e -> listener.run());
        urlSettingsForm.addUrlTypeChangeListeners(listener);
    }

    private boolean isCloudProviderAuthenticationVisible() {
        CloudConfigProviderType provider = urlSettingsForm.getCloudConfigProviderType();
        return urlSettingsForm.isCloudProviderConfig() &&
                provider != null &&
                (provider.isGcp() || provider.isAws() ||
                        CloudConfigProviderAuthentication.values(provider).length > 0);
    }

    private AuthenticationType[] getAuthenticationTypes() {
        DatabaseUrlType urlType = Commons.nvl(urlSettingsForm.getUrlType(), DatabaseUrlType.CUSTOM);
        return isHttpsConfigFile(urlType) ?
                new AuthenticationType[]{NONE, USER_PASSWORD} :
                getSelectedDatabaseType().getAuthTypes();
    }

    private String getCredentialsTitle() {
        if (urlSettingsForm.isCloudProviderConfig()) {
            return txt("cfg.connection.title.CloudProviderCredentials");
        }
        if (isHttpsConfigFile(urlSettingsForm.getUrlType())) {
            return txt("cfg.connection.title.ServerCredentials");
        }
        return txt("cfg.connection.title.DatabaseCredentials");
    }

    private boolean isHttpsConfigFile(DatabaseUrlType urlType) {
        return urlType == DatabaseUrlType.PROVIDER &&
                urlSettingsForm.getConfigSourceType() == ConfigSourceType.URL;
    }

    @Override
    public void resetFormChanges() {
        ConnectionDatabaseSettings configuration = getConfiguration();
        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        DatabaseType databaseType = configuration.getDatabaseType();

        nameTextField.setText(configuration.getDisplayName());
        descriptionTextField.setText(configuration.getDescription());
        setSelection(databaseTypeComboBox, databaseType);

        urlSettingsForm.resetFormChanges();
        authSettingsForm.resetFormChanges();
        urlSettingsForm.updateUrlField();
        driverSettingsForm.resetFormChanges();
    }

    private void updateNativeSupportDatabaseHint() {
        DatabaseType selectedDatabaseType = getSelectedDatabaseType();
        DatabaseType driverDatabaseType = driverSettingsForm.getDriverDatabaseType();
        if (selectedDatabaseType == DatabaseType.GENERIC && driverDatabaseType != null && driverDatabaseType != selectedDatabaseType) {
            String databaseTypeName = driverDatabaseType.getName();
            TextContent hintText = TextContent.plain(txt("cfg.connection.hint.KnownDatabaseType", databaseTypeName));
            DBNHintForm hintForm = new DBNHintForm(this,
                    hintText, null, true,
                    txt("cfg.connection.action.ChangeToDatabaseType", databaseTypeName),
                    () -> setSelection(databaseTypeComboBox, driverDatabaseType));
            hintForm.setHighlighted(true);
            databaseTypeHintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);
        } else {
            databaseTypeHintPanel.removeAll();
        }
    }
}
