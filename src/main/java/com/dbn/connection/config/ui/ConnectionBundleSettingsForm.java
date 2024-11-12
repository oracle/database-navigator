package com.dbn.connection.config.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.clipboard.Clipboard;
import com.dbn.common.color.Colors;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.CardLayouts;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Naming;
import com.dbn.common.util.XmlContents;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.*;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.tns.TnsImportType;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsProfile;
import com.dbn.driver.DriverSource;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.ui.util.Splitters.makeRegular;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

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
        connectionsList.setFont(Fonts.getLabelFont());
        connectionsList.setBackground(Colors.getTextFieldBackground());
        connectionsList.setBorder(Borders.EMPTY_BORDER);
        makeRegular(contentSplitPane);

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "DBNavigator.ActionGroup.ConnectionSettings", "", true);
        JComponent component = actionToolbar.getComponent();
        actionsPanel.add(component, BorderLayout.CENTER);
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
        String selectedTabName = currentForm == null ? null : currentForm.getSelectedTabName();

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

    public void duplicateSelectedConnection() {
        ConnectionSettings connectionSettings = connectionsList.getSelectedValue();
        if (connectionSettings != null) {
            getConfiguration().setModified(true);
            ConnectionSettingsForm settingsEditor = connectionSettings.getSettingsEditor();
            if (settingsEditor != null) {
                try {
                    ConnectionSettings duplicate = settingsEditor.getTemporaryConfig();
                    duplicate.setNew(true);
                    duplicate.setSigned(true);
                    String name = duplicate.getDatabaseSettings().getName();
                    ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
                    while (model.getConnectionConfig(name) != null) {
                        name = Naming.nextNumberedIdentifier(name, true);
                    }
                    duplicate.getDatabaseSettings().setName(name);
                    int selectedIndex = connectionsList.getSelectedIndex() + 1;
                    model.add(selectedIndex, duplicate);
                    connectionsList.setSelectedIndex(selectedIndex);
                } catch (ConfigurationException e) {
                    conditionallyLog(e);
                    Messages.showErrorDialog(getProject(), e.getMessage());
                }
            }

        }
    }

    public void removeSelectedConnections() {
        getConfiguration().setModified(true);
        List<ConnectionSettings> connectionSettings = ListUtil.removeSelectedItems(connectionsList);
        for (ConnectionSettings connectionSetting : connectionSettings) {
            connectionSetting.disposeUIResources();
        }

    }

    public void moveSelectedConnectionsUp() {
        getConfiguration().setModified(true);
        ListUtil.moveSelectedItemsUp(connectionsList);
    }

    public void moveSelectedConnectionsDown() {
        getConfiguration().setModified(true);
        ListUtil.moveSelectedItemsDown(connectionsList);
    }

    public void copyConnectionsToClipboard() {
        List<ConnectionSettings> configurations = connectionsList.getSelectedValuesList();
        Project project = getProject();
        try {
            Element rootElement = newElement("connection-configurations");
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
                    "msg.connection.error.ExportFailed", e);
        }
    }

    public void pasteConnectionsFromClipboard() {
        String clipboardData = Clipboard.getStringContent();
        if (clipboardData != null) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(clipboardData.getBytes())) {
                Element rootElement = XmlContents.streamToElement(inputStream);
                boolean configurationsFound = false;
                List<Element> configElements = rootElement.getChildren();
                ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
                int index = connectionsList.getModel().getSize();
                List<Integer> selectedIndices = new ArrayList<>();
                ConnectionBundleSettings configuration = getConfiguration();
                for (Element configElement : configElements) {
                    ConnectionSettings clone = new ConnectionSettings(configuration);
                    clone.readConfiguration(configElement);
                    clone.setNew(true);
                    clone.generateNewId();

                    ConnectionDatabaseSettings databaseSettings = clone.getDatabaseSettings();
                    String name = databaseSettings.getName();
                    while (model.getConnectionConfig(name) != null) {
                        name = Naming.nextNumberedIdentifier(name, true);
                    }
                    databaseSettings.setName(name);
                    model.add(index, clone);
                    selectedIndices.add(index);
                    configuration.setModified(true);
                    index++;
                    configurationsFound = true;
                }

                if (configurationsFound) {
                    int[] indices = selectedIndices.stream().mapToInt(i -> i).toArray();
                    connectionsList.setSelectedIndices(indices);
                }

                if (!configurationsFound) {
                    Messages.showWarningDialog(
                            getProject(),
                            txt("msg.connection.title.ImportFailed"),
                            txt("msg.connection.warning.ImportFailedEmpty"));
                }

            } catch (Exception e) {
                conditionallyLog(e);
                Messages.showErrorDialog(getProject(),
                        txt("msg.connection.title.ImportFailed"),
                        txt("msg.connection.error.ImportFailedUnparseable"), e);
            }
        }
    }

    public void importTnsNames(TnsImportData importData) {
        ConnectionBundleSettings connectionBundleSettings = getConfiguration();
        ConnectionListModel model = (ConnectionListModel) connectionsList.getModel();
        int index = connectionsList.getModel().getSize();
        List<Integer> selectedIndexes = new ArrayList<>();

        TnsNames tnsNames = importData.getTnsNames();
        List<TnsProfile> tnsProfiles = importData.isSelectedOnly() ? tnsNames.getSelectedProfiles() : tnsNames.getProfiles();
        for (TnsProfile tnsProfile : tnsProfiles) {
            ConnectionSettings connectionSettings = new ConnectionSettings(connectionBundleSettings, DatabaseType.ORACLE, ConnectionConfigType.BASIC);
            connectionSettings.setNew(true);
            connectionSettings.generateNewId();
            connectionBundleSettings.setModified(true);
            connectionBundleSettings.getConnections().add(connectionSettings);

            ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
            DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
            importTnsData(databaseInfo, tnsProfile, tnsNames, importData.getImportType());

            String name = tnsProfile.getProfile();
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
