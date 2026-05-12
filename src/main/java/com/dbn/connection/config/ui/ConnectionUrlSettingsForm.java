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
import com.dbn.connection.config.imports.CloudConfigProviderType;
import com.dbn.connection.config.imports.ConfigFileSourceType;
import com.dbn.connection.config.parameter.ui.UrlParameterInputDialog;
import com.dbn.connection.config.tns.TnsAdmin;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
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
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.FileChoosers.addSingleFolderChooser;
import static com.dbn.common.util.Files.normalizePath;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
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
    private JLabel configFileLabel;
    private JLabel configLocationLabel;
    private JLabel configFileProfileKeyLabel;
    private JLabel gcpStorageProjectLabel;
    private JLabel gcpStorageBucketLabel;
    private JLabel gcpStorageObjectLabel;
    private JPanel databaseFilesPanel;
    private ComboBox<DatabaseUrlType> urlTypeComboBox;
    private JComboBox<ConfigFileSourceType> sourceTypeComboBox;
    private JComboBox<CloudConfigProviderType> cloudProviderComboBox;
    private JComboBox<ServerType> serverTypeComboBox;
    private JComboBox<DatabaseProtocol> protocolComboBox;
    private DBNComboBox<Presentable> tnsProfileComboBox;
    private JTextField hostTextField;
    private JTextField portTextField;
    private JTextField databaseTextField;
    private JTextField configLocationTextField;
    private JTextField gcpStorageProjectTextField;
    private JTextField gcpStorageBucketTextField;
    private JTextField gcpStorageObjectTextField;
    private JTextField configFileProfileKeyTextField;
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
        urlTypeComboBox.addActionListener(e -> updateFieldVisibility());
        sourceTypeComboBox.addActionListener(e -> updateFieldVisibility());
        cloudProviderComboBox.addActionListener(e -> updateFieldVisibility());
        parametersButton.addActionListener(e -> openParametersDialog());

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
        onTextChange(gcpStorageProjectTextField, e -> updateUrlField());
        onTextChange(gcpStorageBucketTextField, e -> updateUrlField());
        onTextChange(gcpStorageObjectTextField, e -> updateUrlField());
        onTextChange(configFileProfileKeyTextField, e -> updateUrlField());
        tnsProfileComboBox.addActionListener(e -> updateUrlField());
        serverTypeComboBox.addActionListener(e -> updateUrlField());
        protocolComboBox.addActionListener(e -> updateUrlField());

        updateTnsProfilesField();
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

    public ConfigFileSourceType getConfigFileSourceType() {
        return Commons.nvl(getSelection(sourceTypeComboBox), ConfigFileSourceType.LOCAL_FILE);
    }

    public CloudConfigProviderType getCloudConfigProviderType() {
        return getSelection(cloudProviderComboBox);
    }

    public String getConfigLocation() {
        ConfigFileSourceType sourceType = getConfigFileSourceType();
        if (sourceType == ConfigFileSourceType.LOCAL_FILE) return getText(configFileTextField);
        if (isGcpStorageConfig()) return getGcpStorageConfigLocation();

        String configLocation = getText(configLocationTextField);
        return sourceType == ConfigFileSourceType.HTTPS || isOciObjectStorageConfig() ?
                DatabaseUrlPattern.normalizeConfigHttpsLocation(configLocation) :
                configLocation;
    }

    private String getGcpStorageConfigLocation() {
        String project = getText(gcpStorageProjectTextField);
        String bucket = getText(gcpStorageBucketTextField);
        String object = getText(gcpStorageObjectTextField);
        if (Strings.isEmptyOrSpaces(project) &&
                Strings.isEmptyOrSpaces(bucket) &&
                Strings.isEmptyOrSpaces(object)) {
            return "";
        }
        return "project=" + project.trim() +
                ";bucket=" + bucket.trim() +
                ";object=" + object.trim();
    }

    public String getConfigFileProfileKey() {
        return getText(configFileProfileKeyTextField);
    }

    public boolean requiresAuthentication() {
        return getUrlType() != DatabaseUrlType.CONFIG_FILE || getConfigFileSourceType() != ConfigFileSourceType.LOCAL_FILE;
    }

    void updateUrlField() {
        DatabaseUrlType urlType = getUrlType();
        if (urlType == DatabaseUrlType.CUSTOM) return;

        DatabaseType databaseType = getDatabaseType();
        DatabaseUrlPattern urlPattern = nvl(databaseType.getUrlPattern(urlType), DatabaseUrlPattern.GENERIC);
        Map<String, String> urlParameters = urlType == DatabaseUrlType.CONFIG_FILE ? getConfigFileParameters() : getParameters();
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
                urlParameters,
                getConfigProvider(),
                getConfigLocation());
        urlTextField.setText(url);
    }

    private String getConfigProvider() {
        ConfigFileSourceType configFileSourceType = getConfigFileSourceType();
        return switch (configFileSourceType) {
            case LOCAL_FILE -> "file";
            case HTTPS -> "https";
            case CLOUD_PROVIDER -> {
                CloudConfigProviderType cloudConfigProviderType = getCloudConfigProviderType();
                yield cloudConfigProviderType == null ? "" : cloudConfigProviderType.getSlug();
            }
        };
    }

    private Map<String, String> getConfigFileParameters() {
        Map<String, String> parameters = new LinkedHashMap<>();

        String profileKey = getConfigFileProfileKey();
        if (Strings.isNotEmptyOrSpaces(profileKey)) {
            parameters.put("key", profileKey.trim());
        }

        return parameters.isEmpty() ? Collections.emptyMap() : parameters;
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
        ConfigFileSourceType configFileSourceType = getConfigFileSourceType();

        boolean ezConnectVisible = urlType == DatabaseUrlType.EZCONNECT;
        boolean tnsVisible = urlType == DatabaseUrlType.TNS;
        boolean flsVisible = urlType == DatabaseUrlType.FILE;
        boolean configFileVisible = urlType == DatabaseUrlType.CONFIG_FILE;
        boolean localConfigFileVisible = configFileVisible && configFileSourceType == ConfigFileSourceType.LOCAL_FILE;
        boolean remoteConfigVisible = configFileVisible && configFileSourceType != ConfigFileSourceType.LOCAL_FILE;
        boolean cloudProviderVisible = configFileVisible && configFileSourceType == ConfigFileSourceType.CLOUD_PROVIDER;
        boolean gcpStorageConfig = remoteConfigVisible && isGcpStorageConfig();
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
        configFileLabel.setVisible(localConfigFileVisible);
        configFileTextField.setVisible(localConfigFileVisible);
        configLocationLabel.setVisible(remoteConfigVisible && !gcpStorageConfig);
        configLocationTextField.setVisible(remoteConfigVisible && !gcpStorageConfig);
        gcpStorageProjectLabel.setVisible(gcpStorageConfig);
        gcpStorageProjectTextField.setVisible(gcpStorageConfig);
        gcpStorageBucketLabel.setVisible(gcpStorageConfig);
        gcpStorageBucketTextField.setVisible(gcpStorageConfig);
        gcpStorageObjectLabel.setVisible(gcpStorageConfig);
        gcpStorageObjectTextField.setVisible(gcpStorageConfig);
        configFileProfileKeyLabel.setVisible(configFileVisible);
        configFileProfileKeyTextField.setVisible(configFileVisible);

        // file based url
        databaseFilesLabel.setVisible(flsVisible);
        databaseFilesPanel.setVisible(flsVisible);

        updateUrlField();
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        parent.updateAuthenticationVisibility();

    }

    boolean isOciCloudProvider() {
        CloudConfigProviderType provider = getCloudConfigProviderType();
        return isCloudProviderConfig() && provider != null && provider.isOci();
    }

    private boolean isOciObjectStorageConfig() {
        return isCloudProviderConfig() &&
                getCloudConfigProviderType() == CloudConfigProviderType.OCI_OBJECT;
    }

    private boolean isGcpStorageConfig() {
        return isCloudProviderConfig() &&
                getCloudConfigProviderType() == CloudConfigProviderType.GCP_STORAGE;
    }

    boolean isCloudProviderConfig() {
        return getUrlType() == DatabaseUrlType.CONFIG_FILE &&
                getConfigFileSourceType() == ConfigFileSourceType.CLOUD_PROVIDER;
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

            histInfo = urlPattern.getDefaultInfo();
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
        databaseInfo.setConfigFileSourceType(getConfigFileSourceType());
        databaseInfo.setCloudConfigProviderType(getCloudConfigProviderType());
        databaseInfo.setConfigLocation(getConfigLocation());
        databaseInfo.setConfigFileProfileKey(getConfigFileProfileKey());
        databaseInfo.setUrlType(getUrlType());
        databaseInfo.setUrl(getUrl());
        databaseInfo.setServerType(getServerType());
        databaseInfo.setParameters(getParameters());
        databaseInfo.setProtocol(getProtocol());
        return databaseInfo;
    }



    private void applyDatabaseInfo(DatabaseInfo databaseInfo) {
        databaseFileSettingsForm.setFileBundle(databaseInfo.getFileBundle());
        hostTextField.setText(databaseInfo.getHost());
        portTextField.setText(databaseInfo.getPort());
        databaseTextField.setText(databaseInfo.getDatabase());
        tnsFolderTextField.setText(databaseInfo.getTnsFolder());
        configFileTextField.setText(databaseInfo.getConfigLocation());
        configLocationTextField.setText(databaseInfo.getConfigLocation());
        applyGcpStorageConfigLocation(databaseInfo.getConfigLocation());
        configFileProfileKeyTextField.setText(databaseInfo.getConfigFileProfileKey());
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

        initComboBox(sourceTypeComboBox, ConfigFileSourceType.values());
        setSelection(sourceTypeComboBox, Commons.nvl(databaseInfo.getConfigFileSourceType(), ConfigFileSourceType.LOCAL_FILE));

        initComboBox(cloudProviderComboBox, CloudConfigProviderType.values());
        setSelection(cloudProviderComboBox, databaseInfo.getCloudConfigProviderType());

        initComboBox(protocolComboBox, true, DatabaseProtocol.values());
        setSelection(protocolComboBox, databaseInfo.getProtocol());

        initComboBox(serverTypeComboBox, ServerType.values());
        setSelection(serverTypeComboBox, databaseInfo.getServerType());

        urlTypeLabel.setVisible(urlTypes.length > 1);
        urlTypeComboBox.setVisible(urlTypes.length > 1);
        urlTextField.setText(databaseInfo.getUrl());
    }

    private void applyGcpStorageConfigLocation(String configLocation) {
        Map<String, String> values = parseGcpStorageConfigLocation(configLocation);
        gcpStorageProjectTextField.setText(values.get("project"));
        gcpStorageBucketTextField.setText(values.get("bucket"));
        gcpStorageObjectTextField.setText(values.get("object"));
    }

    private static Map<String, String> parseGcpStorageConfigLocation(String configLocation) {
        Map<String, String> values = new HashMap<>();
        if (Strings.isEmptyOrSpaces(configLocation)) return values;

        String[] tokens = configLocation.split(";");
        for (String token : tokens) {
            String[] entry = token.split("=", 2);
            if (entry.length != 2) continue;
            values.put(entry[0].trim().toLowerCase(), entry[1].trim());
        }
        return values;
    }

    @NotNull
    private ConnectionDatabaseSettings getDatabaseSettings() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getConfiguration();
    }

    boolean settingsChanged() {
        ConnectionDatabaseSettings configuration = getDatabaseSettings();

        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        DatabaseUrlType urlType = getUrlType();
        return
            !Commons.match(databaseInfo.getHost(), getHost()) ||
            !Commons.match(databaseInfo.getPort(), getPort()) ||
            !Commons.match(databaseInfo.getDatabase(), getDatabase()) ||
            !Commons.match(databaseInfo.getTnsFolder(), getTnsFolder()) ||
            !Commons.match(databaseInfo.getTnsProfile(), getTnsProfile()) ||
            !Commons.match(databaseInfo.getConfigFileSourceType(), getConfigFileSourceType()) ||
            !Commons.match(databaseInfo.getCloudConfigProviderType(), getCloudConfigProviderType()) ||
            !Commons.match(databaseInfo.getConfigLocation(), getConfigLocation()) ||
            !Commons.match(databaseInfo.getConfigFileProfileKey(), getConfigFileProfileKey()) ||
            !Commons.match(databaseInfo.getUrl(), getUrl()) ||
            !Commons.match(databaseInfo.getUrlType(), urlType) ||
            !Commons.match(databaseInfo.getFileBundle(), urlType == DatabaseUrlType.FILE ? getFileBundle() : null);

    }
}
