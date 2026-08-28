/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.constant.Constants;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Safe;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseProtocol;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.ServerType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.EasyConnectParameters;
import com.dbn.connection.config.file.DatabaseFileBundle;
import com.dbn.connection.config.file.ui.DatabaseFileSettingsForm;
import com.dbn.connection.config.parameter.ui.UrlParameterInputDialog;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigSourceType;
import com.dbn.connection.config.tns.TnsAdmin;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.oci.database.tools.OciDatabaseToolsConnectionInfo;
import com.dbn.oci.database.tools.ui.OciDatabaseToolsConnectionDialog;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.ui.util.Buttons.onButtonClick;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setEmptyText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Commons.match;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.FileChoosers.addSingleFolderChooser;
import static com.dbn.common.util.Files.normalizePath;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.connection.config.provider.CloudConfigProviderType.AZURE_APP_CONFIG;
import static com.dbn.connection.config.provider.CloudConfigProviderType.GCP_STORAGE;
import static com.dbn.connection.config.provider.CloudConfigProviderType.OCI_DB_TOOLS;
import static com.dbn.connection.config.provider.CloudConfigProviderType.OCI_OBJECT;
import static com.dbn.connection.config.provider.CloudConfigProviderType.values;
import static com.dbn.connection.config.provider.impl.GcpConfigProviderHandler.getStorageLocation;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static java.util.Collections.unmodifiableMap;

public class ConnectionUrlSettingsForm extends DBNFormBase {
    private JLabel urlTypeLabel;
    private JLabel hostLabelField;
    private JLabel portLabelField;
    private JLabel databaseLabel;
    private JLabel tnsFolderLabel;
    private JLabel tnsProfileLabel;
    private JLabel databaseFilesLabel;
    private JLabel urlLabel;
    private JLabel serverTypeLabel;
    private JLabel protocolLabel;
    private JLabel sourceTypeLabel;
    private JLabel cloudProviderLabel;
    private DBNHyperlinkLabel cloudProviderDocumentationLink;
    private JLabel configFileLabel;
    private JLabel configLocationLabel;
    private JLabel configFileProfileKeyLabel;
    private JLabel azureLabelLabel;
    private JLabel cloudRegionLabel;
    private JLabel gcpStorageProjectLabel;
    private JLabel gcpStorageBucketLabel;
    private JLabel gcpStorageObjectLabel;
    private JPanel databaseFilesPanel;
    private ComboBox<DatabaseUrlType> urlTypeComboBox;
    private JComboBox<ConfigSourceType> sourceTypeComboBox;
    private JComboBox<CloudConfigProviderType> cloudProviderComboBox;
    private JComboBox<ServerType> serverTypeComboBox;
    private JComboBox<DatabaseProtocol> protocolComboBox;
    private DBNComboBox<Presentable> tnsProfileComboBox;
    private JBTextField hostTextField;
    private JBTextField portTextField;
    private JBTextField databaseTextField;
    private JBTextField configLocationTextField;
    private JButton configLocationBrowseButton;
    private JBTextField cloudRegionTextField;
    private JBTextField gcpStorageProjectTextField;
    private JBTextField gcpStorageBucketTextField;
    private JBTextField gcpStorageObjectTextField;
    private JBTextField configFileProfileKeyTextField;
    private JBTextField azureLabelTextField;
    private TextFieldWithBrowseButton tnsFolderTextField;
    private TextFieldWithBrowseButton configFileTextField;
    private ExpandableTextField urlTextField;
    private JPanel mainPanel;
    private JButton parametersButton;

    private final DatabaseFileSettingsForm databaseFileSettingsForm;
    private final Map<DatabaseType, DatabaseInfo> history = new HashMap<>();
    private Map<String, String> parameters = new HashMap<>();


    public ConnectionUrlSettingsForm(ConnectionDatabaseSettingsForm parent, ConnectionDatabaseSettings configuration) {
        super(parent);

        databaseFileSettingsForm = new DatabaseFileSettingsForm(this, configuration.getDatabaseInfo().getFileBundle());
        databaseFilesPanel.add(databaseFileSettingsForm.getComponent(), BorderLayout.CENTER);

        onSelectionChange(urlTypeComboBox, e -> updateFieldVisibility());
        onSelectionChange(sourceTypeComboBox, e -> updateProviderLocation());
        onSelectionChange(cloudProviderComboBox, e -> updateProviderLocation());
        onButtonClick(parametersButton, e -> openParametersDialog());
        onButtonClick(configLocationBrowseButton, e -> openOciDatabaseToolsConnectionDialog());

        updateTnsAdminField();

        addSingleFolderChooser(
                getProject(),
                tnsFolderTextField,
                txt("cfg.connection.title.SelectWalletDirectory"),
                txt("cfg.connection.text.ValidTnsNamesFolder"));
        addSingleFileChooser(getProject(), configFileTextField, txt("cfg.connection.title.SelectConfigFile"), "");

        onTextChange(hostTextField, e -> updateUrlField());
        onTextChange(portTextField, e -> updateUrlField());
        onTextChange(databaseTextField, e -> updateUrlField());
        onTextChange(tnsFolderTextField, e -> updateTnsProfilesField());
        onTextChange(tnsFolderTextField, e -> updateUrlField());
        onTextChange(configFileTextField, e -> updateUrlField());
        onTextChange(configLocationTextField, e -> updateUrlField());
        onTextChange(cloudRegionTextField, e -> updateUrlField());
        onTextChange(gcpStorageProjectTextField, e -> updateUrlField());
        onTextChange(gcpStorageBucketTextField, e -> updateUrlField());
        onTextChange(gcpStorageObjectTextField, e -> updateUrlField());
        onTextChange(configFileProfileKeyTextField, e -> updateUrlField());
        onTextChange(azureLabelTextField, e -> updateUrlField());
        onSelectionChange(tnsProfileComboBox, e -> updateUrlField());
        onSelectionChange(serverTypeComboBox, e -> updateUrlField());
        onSelectionChange(protocolComboBox, e -> updateUrlField());

        updateTnsProfilesField();
    }

    private void updateProviderLocation() {
        getDatabaseSettings().getConfigProviderInfo().setProviderLocation(getText(configLocationTextField));
        updateFieldVisibility();
    }

    private void openParametersDialog() {
        DatabaseType databaseType = getDatabaseType();
        DatabaseUrlPattern urlPattern = databaseType.getUrlPattern(DatabaseUrlType.EZCONNECT);
        if (urlPattern == null) return;

        // ensure that we populate table with empty builtin keys even if the current url doesn't have them.
        // (also retain logical order of the parameters)
        LinkedHashMap<String, String> parameters =
                EasyConnectParameters.ensureParameters(this.parameters, (DatabaseProtocol) this.protocolComboBox.getSelectedItem());

        UrlParameterInputDialog dialog = new UrlParameterInputDialog(getProject(), parameters);
        if (dialog.showAndGet()) {
            this.parameters = dialog.getParameters();
            updateUrlField();
        }
    }

    private void openOciDatabaseToolsConnectionDialog() {
        if (!isOciDatabaseToolsConfig()) return;

        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        OciDatabaseToolsConnectionDialog dialog =
                new OciDatabaseToolsConnectionDialog(getProject(), parent.getOciAuthenticationConfig());
        if (dialog.showAndGet()) {
            OciDatabaseToolsConnectionInfo connection = dialog.getSelectedConnection();
            if (connection != null) configLocationTextField.setText(connection.getId());
        }
    }

    private void updateTnsAdminField() {
        String location = TnsAdmin.location();
        if (isEmptyOrSpaces(location)) return;

        JBTextField textField = (JBTextField) tnsFolderTextField.getTextField();
        textField.getEmptyText().setText(location);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private DatabaseType getDatabaseType() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getSelectedDatabaseType();
    }

    public String getVendor() {
        return toLowerCase(Objects.toString(getDatabaseType()));
    }

    public String getHost() {
        return getText(hostTextField);
    }

    public String getPort() {
        return getText(portTextField);
    }

    public String getDatabase() {
        return getText(databaseTextField);
    }

    public String getTnsFolder() {
        return getText(tnsFolderTextField);
    }

    public String getTnsProfile() {
        return Safe.call(tnsProfileComboBox.getSelectedValue(), v -> v.getName());
    }

    public ServerType getServerType() {
        return getSelection(serverTypeComboBox);
    }

    public String getUrl() {
        return getText(urlTextField);
    }

    public DatabaseFileBundle getFileBundle() {
        return databaseFileSettingsForm.getFileBundle();
    }

    public DatabaseUrlType getUrlType() {
        return getSelection(urlTypeComboBox);
    }

    void addUrlTypeChangeListeners(Runnable listener) {
        urlTypeComboBox.addActionListener(e -> listener.run());
    }

    public ConfigSourceType getConfigSourceType() {
        return Commons.nvl(getSelection(sourceTypeComboBox), ConfigSourceType.FILE);
    }

    public CloudConfigProviderType getCloudConfigProviderType() {
        return getSelection(cloudProviderComboBox);
    }

    public String getConfigLocation() {
        ConfigSourceType sourceType = Commons.nvl(getConfigSourceType(), ConfigSourceType.FILE);
        if (sourceType == ConfigSourceType.FILE) return getText(configFileTextField);
        if (isGcpStorageConfig()) {
            return getStorageLocation(
                    getText(gcpStorageProjectTextField),
                    getText(gcpStorageBucketTextField),
                    getText(gcpStorageObjectTextField));
        }

        String configLocation = getText(configLocationTextField);
        return sourceType == ConfigSourceType.URL || isOciObjectStorageConfig() ?
                DatabaseUrlPattern.normalizeConfigHttpsLocation(configLocation) :
                configLocation;
    }

    public String getConfigFileProfileKey() {
        return getText(configFileProfileKeyTextField);
    }

    public String getAzureLabel() {
        return isAzureAppConfig() ? getText(azureLabelTextField) : null;
    }

    public String getCloudConfigProviderRegion() {
        return isCloudRegionConfig() ? getCloudRegion() : null;
    }

    public boolean requiresAuthentication() {
        return getUrlType() != DatabaseUrlType.PROVIDER || getConfigSourceType() != ConfigSourceType.FILE;
    }

    void updateUrlField() {
        DatabaseUrlType urlType = getUrlType();
        if (urlType == DatabaseUrlType.CUSTOM) return;

        DatabaseType databaseType = getDatabaseType();
        DatabaseUrlPattern urlPattern = nvl(databaseType.getUrlPattern(urlType), DatabaseUrlPattern.GENERIC);
        ConfigProviderInfo configProvider = loadConfigProviderInfo();
        Map<String, String> urlParameters = urlType == DatabaseUrlType.PROVIDER ?
                configProvider.getUrlParameters(false) :
                getParameters();
        String url = urlPattern.buildUrl(
                getVendor(),
                getHost(),
                getPort(),
                getDatabase(),
                getMainFilePath() ,
                getTnsAdmin(),
                getTnsProfile(),
                getProtocol(),
                getServerType(),
                configProvider.getProviderSlug(),
                configProvider.getProviderLocation(),
                urlParameters);
        urlTextField.setText(url);
    }

    private ConfigProviderInfo loadConfigProviderInfo() {
        ConfigProviderInfo configProviderInfo = new ConfigProviderInfo(null);
        applyConfigProviderInfo(configProviderInfo);
        return configProviderInfo;
    }

    public Map<String, String> getParameters() {
        return unmodifiableMap(this.parameters);
    }

    public DatabaseProtocol getProtocol() {
        return getSelection(this.protocolComboBox);
    }

    private String getMainFilePath() {
        return databaseFileSettingsForm.getFileBundle().getMainFilePath();
    }

    private void updateTnsProfilesField() {
        String tnsAdmin = getTnsAdmin();

        String tnsProfile = getTnsProfile();
        // retain profile selection if list is not overwritten by a new set of entries
        List<Presentable> tnsProfiles = isEmpty(tnsProfile) ?
                Collections.emptyList():
                Collections.singletonList(Presentable.basic(tnsProfile));

        tnsProfileComboBox.setValues(tnsProfiles);
        File tnsFolder = new File(tnsAdmin);
        if (!tnsFolder.isDirectory()) return;

        File tnsFile = new File(tnsFolder, "tnsnames.ora");
        if (!tnsFile.exists()) return;

        List<String> tnsEntries = getTnsEntries(tnsFile);
        tnsProfileComboBox.setValues(Presentable.basic(tnsEntries));

    }

    private String getTnsAdmin() {
        String tnsPath = tnsFolderTextField.getText();
        if (isEmptyOrSpaces(tnsPath)) {
            tnsPath = nvl(TnsAdmin.location(), "");
        }
        return normalizePath(tnsPath);
    }

    private List<String> getTnsEntries(File tnsnamesOraFile) {
        try {
            TnsNames tnsNames = TnsNamesParser.get(tnsnamesOraFile);
            return tnsNames.getProfileNames();
        } catch (Exception e) {
            conditionallyLog(e);
            //ErrorHandler.logErrorStack("Error occurred while reading tnsnames.ora file for database: " + adbInstance.getDbName(), e);
        }
        return Collections.emptyList();
    }

    public void updateFieldVisibility() {
        DatabaseUrlType urlType = nvl(getUrlType(), DatabaseUrlType.CUSTOM);
        ConfigSourceType configFileSourceType = getConfigSourceType();

        boolean ezConnectVisible = urlType == DatabaseUrlType.EZCONNECT;
        boolean tnsVisible = urlType == DatabaseUrlType.TNS;
        boolean flsVisible = urlType == DatabaseUrlType.FILE;
        boolean configFileVisible = urlType == DatabaseUrlType.PROVIDER;
        boolean localConfigFileVisible = configFileVisible && configFileSourceType == ConfigSourceType.FILE;
        boolean remoteConfigVisible = configFileVisible && configFileSourceType != ConfigSourceType.FILE;
        boolean cloudProviderVisible = configFileVisible && configFileSourceType == ConfigSourceType.CLOUD;
        boolean gcpStorageConfig = remoteConfigVisible && isGcpStorageConfig();
        boolean cloudRegionConfig = remoteConfigVisible && isCloudRegionConfig();
        boolean azureLabelConfig = remoteConfigVisible && isAzureAppConfig();
        boolean hpdVisible = Constants.isOneOf(urlType,
                DatabaseUrlType.SID,
                DatabaseUrlType.SERVICE,
                DatabaseUrlType.DATABASE,
                DatabaseUrlType.EZCONNECT);

        urlTextField.setEditable(urlType == DatabaseUrlType.CUSTOM);
        urlTextField.setForeground(urlTextField.isEditable() ?
                UIUtil.getTextFieldForeground() :
                // default disabled fg is very hard to read in default dark mode.
                com.dbn.common.color.Colors.lafDarker(
                        UIUtil.getLabelDisabledForeground(), 8));

        // tns folder
        tnsFolderTextField.setVisible(tnsVisible);
        tnsFolderLabel.setVisible(tnsVisible);
        tnsProfileComboBox.setVisible(tnsVisible);
        tnsProfileLabel.setVisible(tnsVisible);

        // classic service name or sid
        databaseLabel.setText(urlType.databaseIdentifier());
        databaseLabel.setVisible(hpdVisible);
        databaseTextField.setVisible(hpdVisible);
        hostLabelField.setVisible(hpdVisible);
        hostTextField.setVisible(hpdVisible);
        portLabelField.setVisible(hpdVisible);
        portTextField.setVisible(hpdVisible);

        serverTypeLabel.setVisible(ezConnectVisible);
        serverTypeComboBox.setVisible(ezConnectVisible);
        protocolLabel.setVisible(ezConnectVisible);
        protocolComboBox.setVisible(ezConnectVisible);
        parametersButton.setVisible(ezConnectVisible);

        sourceTypeLabel.setVisible(configFileVisible);
        sourceTypeComboBox.setVisible(configFileVisible);
        cloudProviderLabel.setVisible(cloudProviderVisible);
        cloudProviderComboBox.setVisible(cloudProviderVisible);
        updateCloudProviderDocumentationLink(cloudProviderVisible);
        configFileLabel.setVisible(localConfigFileVisible);
        configFileTextField.setVisible(localConfigFileVisible);
        updateProviderLocationField();
        configLocationLabel.setVisible(remoteConfigVisible && !gcpStorageConfig);
        configLocationTextField.setVisible(remoteConfigVisible && !gcpStorageConfig);
        configLocationBrowseButton.setVisible(remoteConfigVisible && isOciDatabaseToolsConfig());
        cloudRegionLabel.setVisible(cloudRegionConfig);
        cloudRegionTextField.setVisible(cloudRegionConfig);
        gcpStorageProjectLabel.setVisible(gcpStorageConfig);
        gcpStorageProjectTextField.setVisible(gcpStorageConfig);
        gcpStorageBucketLabel.setVisible(gcpStorageConfig);
        gcpStorageBucketTextField.setVisible(gcpStorageConfig);
        gcpStorageObjectLabel.setVisible(gcpStorageConfig);
        gcpStorageObjectTextField.setVisible(gcpStorageConfig);
        configFileProfileKeyLabel.setVisible(configFileVisible);
        configFileProfileKeyTextField.setVisible(configFileVisible);
        azureLabelLabel.setVisible(azureLabelConfig);
        azureLabelTextField.setVisible(azureLabelConfig);

        // file based url
        databaseFilesLabel.setVisible(flsVisible);
        databaseFilesPanel.setVisible(flsVisible);

        updateUrlField();
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        parent.updateAuthenticationVisibility();

    }

    private void updateProviderLocationField() {
        ConfigProviderInfo configProvider = getDatabaseSettings().getConfigProviderInfo();
        configProvider.setProviderLocation(getText(configLocationTextField));

        ConfigSourceType sourceType = getConfigSourceType();
        CloudConfigProviderType providerType = getCloudConfigProviderType();
        setText(configLocationTextField, configProvider.getProviderLocation(sourceType, providerType));

        setEmptyText(configLocationTextField, resolveConfigLocationName(sourceType));
    }

    private void updateCloudProviderDocumentationLink(boolean cloudProviderVisible) {
        String documentationUrl = getCloudProviderDocumentationUrl(getCloudConfigProviderType());
        cloudProviderDocumentationLink.setHyperlinkText(txt("cfg.connection.link.ProviderDocumentation"));
        cloudProviderDocumentationLink.setVisible(cloudProviderVisible && documentationUrl != null);
        cloudProviderDocumentationLink.setHyperlinkTarget(documentationUrl);
        cloudProviderDocumentationLink.setToolTipText(documentationUrl);
    }

    private static String getCloudProviderDocumentationUrl(CloudConfigProviderType provider) {
        return provider == null ? null : provider.getDocUrl();
    }

    private String resolveConfigLocationName(ConfigSourceType sourceType) {
        if (sourceType == ConfigSourceType.URL) {
            return txt("cfg.connection.label.ProviderConfigLocation_URL");
        }

        if (sourceType != ConfigSourceType.CLOUD) {
            return txt("cfg.connection.label.ConfigLocation");
        }

        CloudConfigProviderType providerType = getCloudConfigProviderType();
        if (providerType == null) return txt("cfg.connection.label.ConfigLocation");

        return providerType.getLocationName();
    }

    boolean isOciCloudProvider() {
        CloudConfigProviderType provider = getCloudConfigProviderType();
        return isCloudProviderConfig() && provider != null && provider.isOci();
    }

    private boolean isOciObjectStorageConfig() {
        return isCloudProviderConfig() && getCloudConfigProviderType() == OCI_OBJECT;
    }

    private boolean isOciDatabaseToolsConfig() {
        return isCloudProviderConfig() && getCloudConfigProviderType() == OCI_DB_TOOLS;
    }

    private boolean isGcpStorageConfig() {
        return isCloudProviderConfig() && getCloudConfigProviderType() == GCP_STORAGE;
    }

    private boolean isAzureAppConfig() {
        return isCloudProviderConfig() && getCloudConfigProviderType() == AZURE_APP_CONFIG;
    }

    private boolean isCloudRegionConfig() {
        CloudConfigProviderType provider = getCloudConfigProviderType();
        return isCloudProviderConfig() && provider != null && provider.getAwsRegionParameterName() != null;
    }

    boolean isCloudProviderConfig() {
        return getUrlType() == DatabaseUrlType.PROVIDER &&
                getConfigSourceType() == ConfigSourceType.CLOUD;
    }

    void handleDatabaseTypeChange(DatabaseType oldDatabaseType, DatabaseType newDatabaseType) {
        DatabaseInfo previousInfo = loadDatabaseInfo();
        history.put(oldDatabaseType, previousInfo);

        DatabaseInfo histInfo = history.get(newDatabaseType);
        if (histInfo == null) {
            String previousUrl = previousInfo.getUrl();
            DatabaseUrlType previousUrlType = previousInfo.getUrlType();

            DatabaseUrlPattern urlPattern = coalesce(
                    () -> newDatabaseType.resolveUrlPattern(previousUrl),
                    () -> newDatabaseType.getUrlPattern(previousUrlType),
                    () -> newDatabaseType.getDefaultUrlPattern());

            histInfo = urlPattern.createDefaultInfo();
            if (Strings.isNotEmptyOrSpaces(previousUrl)) {
                histInfo.setUrl(previousUrl);
                histInfo.initializeDetails(urlPattern);
            }

        }

        applyDatabaseInfo(histInfo);
        updateFieldVisibility();
    }

    public void resetFormChanges() {
        ConnectionDatabaseSettings configuration = getDatabaseSettings();
        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        applyDatabaseInfo(databaseInfo);
        updateFieldVisibility();

    }

    private DatabaseInfo loadDatabaseInfo() {
        DatabaseInfo databaseInfo = new DatabaseInfo();
        databaseInfo.setHost(getHost());
        databaseInfo.setPort(getPort());
        databaseInfo.setDatabase(getDatabase());
        databaseInfo.setFileBundle(getFileBundle().clone());
        databaseInfo.setTnsFolder(getTnsFolder());
        databaseInfo.setTnsProfile(getTnsProfile());
        applyConfigProviderInfo(getDatabaseSettings().getConfigProviderInfo());
        databaseInfo.setUrlType(getUrlType());
        databaseInfo.setUrl(getUrl());
        databaseInfo.setServerType(getServerType());
        databaseInfo.setParameters(getParameters());
        databaseInfo.setProtocol(getProtocol());
        return databaseInfo;
    }

    void applyConfigProviderInfo(ConfigProviderInfo configProviderInfo) {
        ConfigSourceType sourceType = getConfigSourceType();
        CloudConfigProviderType providerType = sourceType == ConfigSourceType.CLOUD ? getCloudConfigProviderType() : null;
        configProviderInfo.setProviderSourceType(sourceType);
        configProviderInfo.setCloudProviderType(providerType);
        configProviderInfo.setAwsRegion(configProviderInfo.isAwsRegionConfig() ? getCloudConfigProviderRegion() : null);
        configProviderInfo.setProviderLocation(getConfigLocation());
        configProviderInfo.setGcpStorageProject(isGcpStorageConfig() ? getText(gcpStorageProjectTextField) : null);
        configProviderInfo.setGcpStorageBucket(isGcpStorageConfig() ? getText(gcpStorageBucketTextField) : null);
        configProviderInfo.setGcpStorageObject(isGcpStorageConfig() ? getText(gcpStorageObjectTextField) : null);
        configProviderInfo.setProviderProfileKey(getConfigFileProfileKey());
        configProviderInfo.setAzureAppConfigLabel(configProviderInfo.isAzureAppConfig() ? getAzureLabel() : null);
    }



    private void applyDatabaseInfo(DatabaseInfo databaseInfo) {
        ConfigProviderInfo configProviderInfo = getDatabaseSettings().getConfigProviderInfo();

        databaseFileSettingsForm.setFileBundle(databaseInfo.getFileBundle());
        hostTextField.setText(databaseInfo.getHost());
        portTextField.setText(databaseInfo.getPort());
        databaseTextField.setText(databaseInfo.getDatabase());
        tnsFolderTextField.setText(databaseInfo.getTnsFolder());
        configFileTextField.setText(configProviderInfo.getProviderLocation());
        configLocationTextField.setText(configProviderInfo.getProviderLocation());
        cloudRegionTextField.setText(configProviderInfo.getAwsRegion());
        applyGcpStorageConfigLocation(configProviderInfo);
        configFileProfileKeyTextField.setText(configProviderInfo.getProviderProfileKey());
        azureLabelTextField.setText(configProviderInfo.getAzureAppConfigLabel());
        parameters = databaseInfo.getParameters();

        String tnsProfile = databaseInfo.getTnsProfile();
        if (Strings.isNotEmpty(tnsProfile)) {
            Presentable presentable = Presentable.basic(tnsProfile);
            tnsProfileComboBox.setSelectedValue(presentable);
        }

        DatabaseType databaseType = getDatabaseType();
        DatabaseUrlType[] urlTypes = databaseType.getUrlTypes();
        initComboBox(urlTypeComboBox, urlTypes);
        setSelection(urlTypeComboBox, databaseInfo.getUrlType());

        initComboBox(sourceTypeComboBox, ConfigSourceType.values());
        setSelection(sourceTypeComboBox, Commons.nvl(configProviderInfo.getProviderSourceType(), ConfigSourceType.FILE));

        initComboBox(cloudProviderComboBox, values());
        setSelection(cloudProviderComboBox, configProviderInfo.getCloudProviderType());

        initComboBox(protocolComboBox, true, DatabaseProtocol.values());
        setSelection(protocolComboBox, databaseInfo.getProtocol());

        initComboBox(serverTypeComboBox, ServerType.values());
        setSelection(serverTypeComboBox, databaseInfo.getServerType());

        urlTypeLabel.setVisible(urlTypes.length > 1);
        urlTypeComboBox.setVisible(urlTypes.length > 1);
        urlTextField.setText(databaseInfo.getUrl());
    }

    private void applyGcpStorageConfigLocation(ConfigProviderInfo configProviderInfo) {
        gcpStorageProjectTextField.setText(configProviderInfo.getGcpStorageProject());
        gcpStorageBucketTextField.setText(configProviderInfo.getGcpStorageBucket());
        gcpStorageObjectTextField.setText(configProviderInfo.getGcpStorageObject());
    }

    @NotNull
    private ConnectionDatabaseSettings getDatabaseSettings() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getConfiguration();
    }

    boolean settingsChanged() {
        ConnectionDatabaseSettings configuration = getDatabaseSettings();

        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        ConfigProviderInfo configProviderInfo = configuration.getConfigProviderInfo();
        DatabaseUrlType urlType = getUrlType();
        return
            !match(databaseInfo.getHost(), getHost()) ||
            !match(databaseInfo.getPort(), getPort()) ||
            !match(databaseInfo.getDatabase(), getDatabase()) ||
            !match(databaseInfo.getTnsFolder(), getTnsFolder()) ||
            !match(databaseInfo.getTnsProfile(), getTnsProfile()) ||
            !match(configProviderInfo.getProviderSourceType(), getConfigSourceType()) ||
            !match(configProviderInfo.getCloudProviderType(), getCloudConfigProviderType()) ||
            !match(configProviderInfo.getAwsRegion(), isCloudRegionConfig() ? getCloudRegion() : null) ||
            !match(configProviderInfo.getProviderLocation(), getConfigLocation()) ||
            !match(configProviderInfo.getProviderProfileKey(), getConfigFileProfileKey()) ||
            !match(configProviderInfo.getAzureAppConfigLabel(), getAzureLabel()) ||
            !match(databaseInfo.getUrl(), getUrl()) ||
            !match(databaseInfo.getUrlType(), urlType) ||
            !match(databaseInfo.getFileBundle(), urlType == DatabaseUrlType.FILE ? getFileBundle() : null);

    }

    private String getCloudRegion() {
        return getText(cloudRegionTextField);
    }
}
