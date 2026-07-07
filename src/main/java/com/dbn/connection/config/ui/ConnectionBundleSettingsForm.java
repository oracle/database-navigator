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

import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.CardLayouts;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionConfigExport;
import com.dbn.connection.config.ConnectionConfigImportPreview;
import com.dbn.connection.config.ConnectionConfigListCellRenderer;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.tns.TnsImportType;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsProfile;
import com.dbn.driver.DriverSource;
import com.dbn.oci.OciConnectionData;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.common.exception.Exceptions.getLocalizedMessage;
import static com.dbn.common.options.ConfigStorage.CLIPBOARD;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Splitters.makeRegular;
import static com.dbn.common.ui.util.Splitters.setSplitPaneProportion;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.anyMatch;
import static com.dbn.common.util.Lists.count;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class ConnectionBundleSettingsForm extends ConfigurationEditorForm<ConnectionBundleSettings> implements ListSelectionListener {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel connectionSetupPanel;
    private JBScrollPane connectionListScrollPane;
    private JSplitPane contentSplitPane;
    private final JList<ConnectionSettings> connectionsList;

    private String currentPanelId;

    
    private final Map<String, ConnectionSettingsForm> cachedForms = DisposableContainers.map(this);

    public JList getList() {
        return connectionsList;
    }

    public ConnectionBundleSettingsForm(ConnectionBundleSettings configuration) {
        super(configuration);
        ConnectionListModel connectionListModel = new ConnectionListModel(configuration);
        connectionsList = new JBList<>(connectionListModel);
        connectionsList.addListSelectionListener(this);
        connectionsList.setCellRenderer(new ConnectionConfigListCellRenderer());
        connectionsList.setBackground(Colors.getTextFieldBackground());
        connectionsList.setBorder(Borders.EMPTY_BORDER);
        makeRegular(contentSplitPane);
        setSplitPaneProportion(contentSplitPane, 0.25);

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBN.Connection.Settings");
        setAccessibleName(actionToolbar, txt("cfg.connections.aria.ConnectionConfigurationActions"));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
        connectionListScrollPane.setViewportView(connectionsList);

        List<ConnectionSettings> connections = configuration.getConnections();
        if (!connections.isEmpty()) {
            selectConnection(connections.get(0).getConnectionId());
        }
        CardLayouts.addBlankCard(connectionSetupPanel, 500, -1);

        Disposer.register(this, connectionListModel);
        //DataProviders.register(mainPanel, this);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConnectionBundleSettings connectionBundleSettings = getConfiguration();

        List<ConnectionSettings> newConnections = new ArrayList<>();
        ConnectionListModel listModel = (ConnectionListModel) connectionsList.getModel();
        for (int i=0; i< listModel.getSize(); i++) {
            ConnectionSettings connection = listModel.getElementAt(i);
            connection.apply();
            connection.setNew(false);
            newConnections.add(connection);
        }

        List<ConnectionSettings> connections = connectionBundleSettings.getConnections();
        connections.clear();
        connections.addAll(newConnections);
    }

    @Override
    public void resetFormChanges() {
        ConnectionListModel listModel = (ConnectionListModel) connectionsList.getModel();
        for (int i=0; i< listModel.getSize(); i++) {
            ConnectionSettings connectionSettings = listModel.getElementAt(i);
            connectionSettings.reset();
        }
    }

    public void selectConnection(@Nullable ConnectionId connectionId) {
        if (connectionId == null) return;

        ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
        for (int i=0; i<model.size(); i++) {
            ConnectionSettings connectionSettings = model.getElementAt(i);
            if (connectionSettings.getConnectionId() == connectionId) {
                connectionsList.setSelectedValue(connectionSettings, true);
                break;
            }
        }

    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        List<ConnectionSettings> selectedValues = connectionsList.getSelectedValuesList();
        if (selectedValues.size() == 1) {
            ConnectionSettings connectionSettings = selectedValues.get(0);
            switchSettingsPanel(connectionSettings);
        } else {
            switchSettingsPanel(null);
        }
    }

    private void switchSettingsPanel(ConnectionSettings connectionSettings) {
        if (connectionSettings == null) {
            CardLayouts.showBlankCard(connectionSetupPanel);
            return;
        }

        ConnectionSettingsForm currentForm = cachedForms.get(currentPanelId);
        // inform the current form that its tab is
        // being replaced with a different selection
        String selectedTabName = null;
        if (currentForm != null) {
            selectedTabName = currentForm.getSelectedTabName();
        }

        currentPanelId = connectionSettings.getConnectionId().id();
        if (!cachedForms.containsKey(currentPanelId)) {
            JComponent setupPanel = connectionSettings.createComponent();
            CardLayouts.addCard(connectionSetupPanel, setupPanel, currentPanelId);
            cachedForms.put(currentPanelId, connectionSettings.getSettingsEditor());
        }

        ConnectionSettingsForm settingsEditor = connectionSettings.getSettingsEditor();
        if (settingsEditor != null) {
            settingsEditor.selectTab(selectedTabName);
        }

        CardLayouts.showCard(connectionSetupPanel, currentPanelId);
    }


    public ConnectionId createNewConnection(@NotNull DatabaseType databaseType, @NotNull ConnectionConfigType configType) {
        ConnectionBundleSettings connectionBundleSettings = getConfiguration();
        ConnectionSettings connectionSettings = new ConnectionSettings(connectionBundleSettings, databaseType, configType);
        connectionSettings.setNew(true);
        connectionSettings.generateNewId();

        connectionBundleSettings.setModified(true);
        connectionBundleSettings.getConnections().add(connectionSettings);

        String name = "Connection";
        ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
        while (model.getConnectionConfig(name) != null) {
            name = Naming.nextNumberedIdentifier(name, true);
        }
        ConnectionDatabaseSettings connectionConfig = connectionSettings.getDatabaseSettings();
        connectionConfig.setName(name);
        int index = connectionsList.getModel().getSize();
        model.add(index, connectionSettings);
        connectionsList.setSelectedIndex(index);
        return connectionSettings.getConnectionId();
    }

    public ConnectionId createNewConnection(@NotNull DatabaseType databaseType, @NotNull ConnectionConfigType configType, OciConnectionData ociConnectionData) {
        ConnectionBundleSettings connectionBundleSettings = getConfiguration();
        ConnectionSettings connectionSettings = new ConnectionSettings(connectionBundleSettings, databaseType, configType, ociConnectionData.getOcid());
        connectionSettings.setNew(true);
        connectionSettings.generateNewId();
        connectionBundleSettings.setModified(true);
        connectionBundleSettings.getConnections().add(connectionSettings);

        String name = ociConnectionData.getConnectionName() ;
        ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
        while (model.getConnectionConfig(name) != null) {
            name = Naming.nextNumberedIdentifier(name, true);
        }
        ConnectionDatabaseSettings connectionConfig = connectionSettings.getDatabaseSettings();
        connectionConfig.setName(name);
        connectionConfig.setConfigType(configType);



        DatabaseInfo databaseInfo = connectionConfig.getDatabaseInfo();
        String dbUrl = getUrl(ociConnectionData);
        databaseInfo.setUrl(dbUrl);
        // TODO: Azure token values?
        connectionConfig.getAuthenticationInfo().setTokenConfigFile(ociConnectionData.getConfigFile());
        connectionConfig.getAuthenticationInfo().setTokenProfile(ociConnectionData.getConfigProfile());
        databaseInfo.setUrlType(DatabaseUrlType.CUSTOM);
        DatabaseUrlPattern urlPattern = Commons.nvl(databaseType.resolveUrlPattern(dbUrl), DatabaseUrlPattern.ORACLE_SERVICE);
        databaseInfo.initializeDetails(urlPattern);
        connectionConfig.setUrlPattern(urlPattern);

        int index = connectionsList.getModel().getSize();
        model.add(index, connectionSettings);
        connectionsList.setSelectedIndex(index);
        ConnectionId connectionId =  connectionSettings.getConnectionId();
        ociConnectionData.setConnectionId(connectionId);
        return connectionId;
    }

    @NonNls
    private String  getUrl(OciConnectionData connectionData){
        String urlPrefix = "jdbc:oracle:thin:@tcps://";
        String connectionStringHigh = connectionData.getAllConnectionStrings().get("HIGH");
      return urlPrefix + connectionStringHigh;
    }

    public void duplicateSelectedConnection() {
        ConnectionSettings connectionSettings = connectionsList.getSelectedValue();
        if (connectionSettings == null) return;

        ConnectionSettingsForm settingsEditor = connectionSettings.getSettingsEditor();
        if (settingsEditor == null) return;

        mackConfigModified();

        try {
            ConnectionSettings duplicate = settingsEditor.getTemporaryConfig();
            duplicate.setNew(true);
            duplicate.setSigned(true);
            ConnectionDatabaseSettings databaseSettings = duplicate.getDatabaseSettings();
            databaseSettings.getAuthenticationInfo().setTemporary(false);

            String name = databaseSettings.getName();
            ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
            while (model.getConnectionConfig(name) != null) {
                name = Naming.nextNumberedIdentifier(name, true);
            }
            databaseSettings.setName(name);
            int selectedIndex = connectionsList.getSelectedIndex() + 1;
            model.add(selectedIndex, duplicate);
            connectionsList.setSelectedIndex(selectedIndex);
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), getLocalizedMessage(e));
        }
    }

    public void changeSelectionEnabledStatus(boolean enabled) {
        mackConfigModified();
        List<ConnectionSettings> selectedSettings = connectionsList.getSelectedValuesList();
        for (ConnectionSettings connectionSettings : selectedSettings) {
            connectionSettings.setActive(enabled);

            ConnectionDatabaseSettingsForm settingsEditor = connectionSettings.getDatabaseSettings().getSettingsEditor();
            if (settingsEditor != null) {
                settingsEditor.notifyPresentationChanges();
            }
        }
        UserInterface.repaint(connectionsList);
    }

    public boolean isSelectionDominantlyEnabled() {
        List<ConnectionSettings> selectedSettings = connectionsList.getSelectedValuesList();
        if (selectedSettings.isEmpty()) return true;
        if (selectedSettings.size() == 1) return selectedSettings.get(0).isActive();

        int activeCount = count(selectedSettings, c -> c.isActive());
        int inactiveCount = count(selectedSettings, c -> !c.isActive());
        return activeCount >= inactiveCount;
    }

    public void removeSelectedConnections() {
        mackConfigModified();
        List<ConnectionSettings> connectionSettings = ListUtil.removeSelectedItems(connectionsList);
        for (ConnectionSettings connectionSetting : connectionSettings) {
            connectionSetting.disposeUIResources();
        }

    }

    public void moveSelectedConnectionsUp() {
        mackConfigModified();
        ListUtil.moveSelectedItemsUp(connectionsList);
    }

    public void moveSelectedConnectionsDown() {
        mackConfigModified();
        ListUtil.moveSelectedItemsDown(connectionsList);
    }

    public void copyConnectionsToClipboard() {
        List<ConnectionSettings> configurations = connectionsList.getSelectedValuesList();
        Project project = getProject();
        try {
            ConfigMonitor.set(CLIPBOARD, true);
            Element rootElement = ConnectionConfigExport.createConnectionConfigElement();
            for (ConnectionSettings configuration : configurations) {
                Element configElement = newElement(rootElement, "config");
                configuration.writeConfiguration(configElement);
            }

            Document document = new Document(rootElement);
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            String xmlString = outputter.outputString(document);

            CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
            copyPasteManager.setContents(new StringSelection(xmlString));
            Messages.showInfoDialog(project,
                    txt("msg.connection.title.ConfigExported"),
                    txt("msg.connection.info.ConfigExported"));
        } catch (Exception e) {
            conditionallyLog(e);
            Messages.showErrorDialog(project,
                    txt("msg.connection.title.ExportFailed"),
                    txt("msg.connection.error.ExportFailed"), e);
        } finally {
            ConfigMonitor.set(CLIPBOARD, false);
        }
    }

    public void pasteConnectionsFromClipboard() {
        try {
            Element rootElement = ConnectionConfigExport.readClipboardElement();
            if (rootElement == null) return;

            List<Element> configElements = rootElement.getChildren();
            ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
            ConnectionBundleSettings configuration = getConfiguration();
            List<ConnectionSettings> importedConnections = new ArrayList<>();
            for (Element configElement : configElements) {
                ConnectionSettings clone = new ConnectionSettings(configuration);
                clone.readConfiguration(configElement);
                clone.setNew(true);
                clone.generateNewId();

                ConnectionDatabaseSettings databaseSettings = clone.getDatabaseSettings();
                databaseSettings.setName(ensureUniqueName(databaseSettings.getName(), model, importedConnections));
                importedConnections.add(clone);
            }

            if (importedConnections.isEmpty()) {
                Messages.showWarningDialog(
                        getProject(),
                        txt("msg.connection.title.ImportFailed"),
                        txt("msg.connection.warning.ImportFailedEmpty"));
                return;
            }

            if (!ConnectionConfigImportPreview.confirm(getProject(), importedConnections)) return;

            int index = connectionsList.getModel().getSize();
            List<Integer> selectedIndices = new ArrayList<>();
            for (ConnectionSettings connection : importedConnections) {
                model.add(index, connection);
                selectedIndices.add(index);
                configuration.setModified(true);
                index++;
            }

            int[] indices = selectedIndices.stream().mapToInt(i -> i).toArray();
            connectionsList.setSelectedIndices(indices);

        } catch (Exception e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(),
                    txt("msg.connection.title.ImportFailed"),
                    txt("msg.connection.error.ImportFailedUnparseable"), e);
        }
    }

    private static String ensureUniqueName(
            String name,
            ConnectionListModel model,
            List<ConnectionSettings> importedConnections) {
        while (true) {
            String candidateName = name;
            boolean nameAlreadyUsed =
                    model.getConnectionConfig(candidateName) != null ||
                    anyMatch(importedConnections, connection -> Commons.match(connection.getDatabaseSettings().getName(), candidateName));
            if (!nameAlreadyUsed) return candidateName;

            name = Naming.nextNumberedIdentifier(candidateName, true);
        }
    }

    public ConnectionId importTnsNames(TnsImportData importData){
        return importTnsNames(importData,null);
    }
    public ConnectionId importTnsNames(TnsImportData importData, OciConnectionData ociConnectionData) {
        ConnectionBundleSettings connectionBundleSettings = getConfiguration();
        ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
        int index = connectionsList.getModel().getSize();
        List<Integer> selectedIndexes = new ArrayList<>();

        ConnectionId firstConnectionId = null;
        TnsNames tnsNames = importData.getTnsNames();
        List<TnsProfile> tnsProfiles = importData.isSelectedOnly() ? tnsNames.getSelectedProfiles() : tnsNames.getProfiles();
        for (TnsProfile tnsProfile : tnsProfiles) {
            ConnectionSettings connectionSettings = getConnectionSettings(ociConnectionData, connectionBundleSettings);
            connectionBundleSettings.setModified(true);
            connectionBundleSettings.getConnections().add(connectionSettings);
            firstConnectionId = firstConnectionId == null ? connectionSettings.getConnectionId() : firstConnectionId;

            ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
            DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
            importTnsData(databaseInfo, tnsProfile, tnsNames, importData.getImportType());
            String name;
            if (ociConnectionData != null) {
                name = ociConnectionData.getConnectionName();
                ociConnectionData.setConnectionId(databaseSettings.getConnectionId());
                databaseSettings.getAuthenticationInfo().setTokenConfigFile(ociConnectionData.getConfigFile());
                databaseSettings.getAuthenticationInfo().setTokenProfile(ociConnectionData.getConfigProfile());
                // TODO handle AZURE token cases
            }else {
                name = tnsProfile.getProfile();
            }
            while (model.getConnectionConfig(name) != null) {
                name = Naming.nextNumberedIdentifier(name, true);
            }
            databaseSettings.setName(name);
            databaseSettings.setDatabaseType(DatabaseType.ORACLE);
            databaseSettings.setDriverSource(DriverSource.BUNDLED);

            model.add(index, connectionSettings);
            selectedIndexes.add(index);
            connectionBundleSettings.setModified(true);
            index++;
        }

        connectionsList.setSelectedIndices(selectedIndexes.stream().mapToInt(i -> i).toArray());
        return firstConnectionId;
    }

    private static @NotNull ConnectionSettings getConnectionSettings(OciConnectionData ociConnectionData, ConnectionBundleSettings connectionBundleSettings) {
        ConnectionSettings connectionSettings = null;
        if (ociConnectionData == null) {
            connectionSettings = new ConnectionSettings(connectionBundleSettings, DatabaseType.ORACLE, ConnectionConfigType.BASIC);
        }else {
            connectionSettings = new ConnectionSettings(connectionBundleSettings, DatabaseType.ORACLE, ConnectionConfigType.BASIC, ociConnectionData.getOcid());

        }
        connectionSettings.setNew(true);
        connectionSettings.generateNewId();
        return connectionSettings;
    }

    private static void importTnsData(DatabaseInfo databaseInfo, TnsProfile tnsName, TnsNames tnsNames, TnsImportType importType) {
        if (importType == TnsImportType.FIELDS) {
            databaseInfo.setHost(tnsName.getHost());
            databaseInfo.setPort(tnsName.getPort());

            String sid = tnsName.getSid();
            String service = tnsName.getServiceName();
            if (isNotEmpty(sid)) {
                databaseInfo.setDatabase(sid);
                databaseInfo.setUrlType(DatabaseUrlType.SID);
            } else if (isNotEmpty(service)) {
                databaseInfo.setDatabase(service);
                databaseInfo.setUrlType(DatabaseUrlType.SERVICE);
            }
        } else if (importType == TnsImportType.PROFILE) {
            databaseInfo.setUrlType(DatabaseUrlType.TNS);
            String tnsFolder = tnsNames.getTnsFolder();
            String tnsProfile = nvl(tnsName.getProfile(), "");

            databaseInfo.setTnsFolder(tnsFolder);
            databaseInfo.setTnsProfile(tnsProfile);
            databaseInfo.setHost(null);
            databaseInfo.setPort(null);
            databaseInfo.setDatabase(null);
        } else if (importType == TnsImportType.DESCRIPTOR){
            databaseInfo.setUrlType(DatabaseUrlType.CUSTOM);
            String url = "jdbc:oracle:thin:@" + tnsName.getDescriptor().replaceAll("\\s", "");

            databaseInfo.setUrl(url);
            databaseInfo.setHost(null);
            databaseInfo.setPort(null);
            databaseInfo.setDatabase(null);
        }
    }


    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.CONNECTION_BUNDLE_SETTINGS.is(dataId)) return this;
        return null;
    }

    public int getSelectionSize() {
        return connectionsList.getSelectedValuesList().size();
    }
}
