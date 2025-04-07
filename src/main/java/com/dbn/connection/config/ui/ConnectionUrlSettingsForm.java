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
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.file.DatabaseFileBundle;
import com.dbn.connection.config.file.ui.DatabaseFileSettingsForm;
import com.dbn.connection.config.parameter.ui.UrlParameterInputDialog;
import com.dbn.connection.config.tns.TnsAdmin;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
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
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.FileChoosers.addSingleFolderChooser;
import static com.dbn.common.util.Files.normalizePath;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.Collections.unmodifiableMap;

public class ConnectionUrlSettingsForm extends DBNFormBase {

    @NonNls
    public static final List<String> EASY_CONNECT_PARAMETER_NAMES = List.of(
            "ENABLE",
            "FAILOVER",
            "LOAD_BALANCE",
            "RECV_BUF_SIZE",
            "SEND_BUF_SIZE",
            "SDU",
            "SOURCE_ROUTE",
            "RETRY_COUNT",
            "RETRY_DELAY",
            "HTTPS_PROXY",
            "HTTPS_PROXY_PORT",
            "WALLET_LOCATION");

    @NonNls
    public static final List<String> EASY_CONNECT_TCPS_ONLY_PARAMETER_NAMES = List.of(
            "SSL_SERVER_DN_MATCH",
            "SSL_SERVER_CERT_DN");

    @NonNls
    public static final List<String> EASY_CONNECT_BOOLEAN_LIKE_STRING_VALUES = List.of(
            "on", "off", "ON", "OFF", "true", "false", "TRUE", "FALSE", "yes", "no", "YES", "NO");

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
    private JPanel databaseFilesPanel;
    private ComboBox<DatabaseUrlType> urlTypeComboBox;
    private JComboBox<String> serverTypeComboBox;
    private JComboBox<DatabaseProtocol> protocolComboBox;
    private DBNComboBox<Presentable> tnsProfileComboBox;
    private JTextField hostTextField;
    private JTextField portTextField;
    private JTextField databaseTextField;
    private TextFieldWithBrowseButton tnsFolderTextField;
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
        parametersButton.addActionListener(e -> openParametersDialog());

        updateTnsAdminField();

        addSingleFolderChooser(
                getProject(), tnsFolderTextField,
                txt("cfg.connection.title.SelectWalletDirectory"),
                txt("cfg.connection.text.ValidTnsNamesFolder"));

        onTextChange(hostTextField, e -> updateUrlField());
        onTextChange(portTextField, e -> updateUrlField());
        onTextChange(databaseTextField, e -> updateUrlField());
        onTextChange(tnsFolderTextField, e -> updateTnsProfilesField());
        onTextChange(tnsFolderTextField, e -> updateUrlField());
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
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        EASY_CONNECT_PARAMETER_NAMES.forEach(key -> parameters.put(key, ""));
        if (this.protocolComboBox.getSelectedItem() == DatabaseProtocol.TCPS) {
            EASY_CONNECT_TCPS_ONLY_PARAMETER_NAMES.forEach(key -> parameters.put(key, ""));
        }
        parameters.putAll(this.parameters);

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
        return hostTextField.getText();
    }

    public String getPort() {
        return portTextField.getText();
    }

    public String getDatabase() {
        return databaseTextField.getText();
    }

    public String getTnsFolder() {
        return tnsFolderTextField.getText();
    }

    public String getTnsProfile() {
        return Safe.call(tnsProfileComboBox.getSelectedValue(), v -> v.getName());
    }

    public String getServerType() {
        return getSelection(serverTypeComboBox);
    }

    public String getUrl() {
        return urlTextField.getText();
    }

    public DatabaseFileBundle getFileBundle() {
        return databaseFileSettingsForm.getFileBundle();
    }

    public DatabaseUrlType getUrlType() {
        return getSelection(urlTypeComboBox);
    }

    private void updateUrlField() {
        DatabaseUrlType urlType = getUrlType();
        if (urlType == DatabaseUrlType.CUSTOM) return;
        // if Easy Connect and we have changed to a non-TCPS protocol, clear the related values
        if (urlType == DatabaseUrlType.EZCONNECT && protocolComboBox.getSelectedItem() != DatabaseProtocol.TCPS) {
            EASY_CONNECT_TCPS_ONLY_PARAMETER_NAMES.forEach(key -> this.parameters.remove(key));
        }
        DatabaseType databaseType = getDatabaseType();
        DatabaseUrlPattern urlPattern = nvl(databaseType.getUrlPattern(urlType), DatabaseUrlPattern.GENERIC);
        String url = urlPattern.buildUrl(
                getVendor(),
                getHost(),
                getPort(),
                getDatabase(),
                getMainFilePath() ,
                getTnsAdmin(),
                getTnsProfile(),
                getServerType(),
                getParameters(),
                getProtocol());
        urlTextField.setText(url);
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

        boolean ezConnectVisible = urlType == DatabaseUrlType.EZCONNECT;
        boolean tnsVisible = urlType == DatabaseUrlType.TNS;
        boolean flsVisible = urlType == DatabaseUrlType.FILE;
        boolean hpdVisible = Constants.isOneOf(urlType,
                DatabaseUrlType.SID,
                DatabaseUrlType.SERVICE,
                DatabaseUrlType.DATABASE,
                DatabaseUrlType.EZCONNECT);

        urlTextField.setEditable(urlType == DatabaseUrlType.CUSTOM);
        urlTextField.setForeground(urlTextField.isEditable() ?
                UIUtil.getTextFieldForeground() :
                UIUtil.getLabelDisabledForeground());

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

        // file based url
        databaseFilesLabel.setVisible(flsVisible);
        databaseFilesPanel.setVisible(flsVisible);

        updateUrlField();
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

    void resetFormChanges() {
        ConnectionDatabaseSettings configuration = getDatabaseSettings();
        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        applyDatabaseInfo(databaseInfo);

    }

    private DatabaseInfo loadDatabaseInfo() {
        DatabaseInfo databaseInfo = new DatabaseInfo();
        databaseInfo.setHost(getHost());
        databaseInfo.setPort(getPort());
        databaseInfo.setDatabase(getDatabase());
        databaseInfo.setFileBundle(getFileBundle().clone());
        databaseInfo.setTnsFolder(getTnsFolder());
        databaseInfo.setTnsProfile(getTnsProfile());
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

        initComboBox(protocolComboBox, true, DatabaseProtocol.values());
        setSelection(protocolComboBox, databaseInfo.getProtocol());

        setSelection(serverTypeComboBox, databaseInfo.getServerType());

        urlTypeLabel.setVisible(urlTypes.length > 1);
        urlTypeComboBox.setVisible(urlTypes.length > 1);
        urlTextField.setText(databaseInfo.getUrl());
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
            !Commons.match(databaseInfo.getUrl(), getUrl()) ||
            !Commons.match(databaseInfo.getUrlType(), urlType) ||
            !Commons.match(databaseInfo.getFileBundle(), urlType == DatabaseUrlType.FILE ? getFileBundle() : null);

    }
}
