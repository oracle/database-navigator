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

import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.environment.EnvironmentTypeBundle;
import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.environment.options.listener.EnvironmentConfigLocalListener;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.message.MessageType;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandlerStatusListener;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionDetailSettings;
import com.dbn.options.general.GeneralProjectSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class ConnectionDetailSettingsForm extends ConfigurationEditorForm<ConnectionDetailSettings> {
    private JPanel mainPanel;
    private JComboBox<CharsetOption> encodingComboBox;
    private JComboBox<EnvironmentType> environmentTypesComboBox;
    private JPanel generalGroupPanel;
    private JPanel autoConnectHintPanel;
    private JTextField connectivityTimeoutTextField;
    private JTextField maxPoolSizeTextField;
    private JTextField idleTimeTextField;
    private JTextField idleTimePoolTextField;
    private JTextField alternativeStatementDelimiterTextField;
    private JTextField passwordExpiryTextField;
    private JCheckBox databaseLoggingCheckBox;
    private JCheckBox sessionManagementCheckBox;
    private JCheckBox ddlFileBindingCheckBox;
    private JCheckBox autoConnectCheckBox;
    private JCheckBox restoreWorkspaceCheckBox;
    private JCheckBox restoreWorkspaceDeepCheckBox;

    public ConnectionDetailSettingsForm(ConnectionDetailSettings configuration) {
        super(configuration);

        initComboBox(encodingComboBox, CharsetOption.ALL);

        List<EnvironmentType> environmentTypes = new ArrayList<>(getEnvironmentTypes());
        environmentTypes.add(0, EnvironmentType.DEFAULT);
        initComboBox(environmentTypesComboBox, environmentTypes);
        resetFormChanges();

        registerComponent(mainPanel);

        environmentTypesComboBox.addActionListener(e -> notifyPresentationChanges());

        // TODO NLS
        TextContent autoConnectHintText = plain(txt("cfg.connection.hint.DisabledAutoConnect"));
        DBNHintForm hintForm = new DBNHintForm(this, autoConnectHintText, MessageType.INFO, false);
        autoConnectHintPanel.add(hintForm.getComponent());

        boolean visibleHint = !autoConnectCheckBox.isSelected() && restoreWorkspaceCheckBox.isSelected();
        autoConnectHintPanel.setVisible(visibleHint);

        ProjectEvents.subscribe(ensureProject(), this, EnvironmentConfigLocalListener.TOPIC, presentationChangeListener);
    }

    private void notifyPresentationChanges() {
        Project project = getConfiguration().getProject();
        EnvironmentType environmentType = getSelection(environmentTypesComboBox);
        Color color = environmentType == null ? null : environmentType.getColor();
        ConnectionId connectionId = getConfiguration().getConnectionId();

        ProjectEvents.notify(project,
                ConnectionPresentationChangeListener.TOPIC,
                (listener) -> listener.presentationChanged(null, null, color, connectionId, null));
    }

    public EnvironmentType getSelectedEnvironmentType() {
        return getSelection(environmentTypesComboBox);
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            Object source = e.getSource();
            if (source == autoConnectCheckBox || source == restoreWorkspaceCheckBox){
                boolean visibleHint = !autoConnectCheckBox.isSelected() && restoreWorkspaceCheckBox.isSelected();
                autoConnectHintPanel.setVisible(visibleHint);
            }
            if (source == restoreWorkspaceCheckBox) {
                restoreWorkspaceDeepCheckBox.setEnabled(restoreWorkspaceCheckBox.isSelected());
                if (!restoreWorkspaceCheckBox.isSelected()) {
                    restoreWorkspaceDeepCheckBox.setSelected(false);
                }
            }
            getConfiguration().setModified(true);
        };
    }

    private List<EnvironmentType> getEnvironmentTypes() {
        Project project = getConfiguration().getProject();
        EnvironmentSettings environmentSettings = GeneralProjectSettings.getInstance(project).getEnvironmentSettings();
        return environmentSettings.getEnvironmentTypes().getEnvironmentTypes();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        final ConnectionDetailSettings configuration = getConfiguration();

        EnvironmentType newEnvironmentType = Commons.nvl(getSelection(environmentTypesComboBox), EnvironmentType.DEFAULT);
        final EnvironmentTypeId newEnvironmentTypeId = newEnvironmentType.getId();

        Charset charset = configuration.getCharset();
        Charset newCharset = getSelection(encodingComboBox).getCharset();
        boolean settingsChanged = !charset.equals(newCharset);

        EnvironmentTypeId environmentTypeId = configuration.getEnvironmentType().getId();
        boolean environmentChanged = environmentTypeId != newEnvironmentTypeId;


        applyFormChanges(configuration);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (environmentChanged) {
                ProjectEvents.notify(project,
                        EnvironmentManagerListener.TOPIC,
                        (listener) -> listener.configurationChanged(project));
            }

            if (settingsChanged) {
                ConnectionId connectionId = configuration.getConnectionId();
                ProjectEvents.notify(project, ConnectionHandlerStatusListener.TOPIC,
                        (listener) -> listener.statusChanged(connectionId));
            }
        });
    }

    @Override
    public void applyFormChanges(ConnectionDetailSettings configuration) throws ConfigurationException {
        CharsetOption charsetOption = getSelection(encodingComboBox);
        EnvironmentType environmentType = getSelection(environmentTypesComboBox);

        configuration.setEnvironmentTypeId(environmentType == null ? EnvironmentTypeId.DEFAULT : environmentType.getId());
        configuration.setCharset(charsetOption == null ? null : charsetOption.getCharset());
        configuration.setRestoreWorkspace(restoreWorkspaceCheckBox.isSelected());
        configuration.setRestoreWorkspaceDeep(restoreWorkspaceDeepCheckBox.isSelected());
        configuration.setConnectAutomatically(autoConnectCheckBox.isSelected());
        configuration.setEnableSessionManagement(sessionManagementCheckBox.isSelected());
        configuration.setEnableDdlFileBinding(ddlFileBindingCheckBox.isSelected());
        configuration.setEnableDatabaseLogging(databaseLoggingCheckBox.isSelected());
        configuration.setAlternativeStatementDelimiter(alternativeStatementDelimiterTextField.getText());
        int connectivityTimeout = ConfigurationEditors.validateIntegerValue(connectivityTimeoutTextField, txt("cfg.connection.field.ConnectivityTimeout"), true, 0, 30, "");
        int idleTimeToDisconnect = ConfigurationEditors.validateIntegerValue(idleTimeTextField, txt("cfg.connection.field.IdleTimeToDisconnect"), true, 0, 60, "");
        int idleTimeToDisconnectPool = ConfigurationEditors.validateIntegerValue(idleTimePoolTextField, txt("cfg.connection.field.IdleTimeToDisconnectPool"), true, 1, 60, "");
        int passwordExpiryTime = ConfigurationEditors.validateIntegerValue(passwordExpiryTextField, txt("cfg.connection.field.IdleTimeToRequestPassword"), true, 0, 60, "");
        int maxPoolSize = ConfigurationEditors.validateIntegerValue(maxPoolSizeTextField, txt("cfg.connection.field.MaxConnectionPoolSize"), true, 3, 20, "");
        configuration.setConnectivityTimeoutSeconds(connectivityTimeout);
        configuration.setIdleMinutesToDisconnect(idleTimeToDisconnect);
        configuration.setIdleMinutesToDisconnectPool(idleTimeToDisconnectPool);
        configuration.setCredentialExpiryMinutes(passwordExpiryTime);
        configuration.setMaxConnectionPoolSize(maxPoolSize);
    }

    @Override
    public void resetFormChanges() {
        ConnectionDetailSettings configuration = getConfiguration();
        setSelection(encodingComboBox, CharsetOption.get(configuration.getCharset()));
        sessionManagementCheckBox.setSelected(configuration.isEnableSessionManagement());
        ddlFileBindingCheckBox.setSelected(configuration.isEnableDdlFileBinding());
        databaseLoggingCheckBox.setSelected(configuration.isEnableDatabaseLogging());
        autoConnectCheckBox.setSelected(configuration.isConnectAutomatically());
        restoreWorkspaceCheckBox.setSelected(configuration.isRestoreWorkspace());
        restoreWorkspaceDeepCheckBox.setSelected(configuration.isRestoreWorkspaceDeep());
        setSelection(environmentTypesComboBox, configuration.getEnvironmentType());
        connectivityTimeoutTextField.setText(Integer.toString(configuration.getConnectivityTimeoutSeconds()));
        idleTimeTextField.setText(Integer.toString(configuration.getIdleMinutesToDisconnect()));
        idleTimePoolTextField.setText(Integer.toString(configuration.getIdleMinutesToDisconnectPool()));
        passwordExpiryTextField.setText(Integer.toString(configuration.getCredentialExpiryMinutes()));
        maxPoolSizeTextField.setText(Integer.toString(configuration.getMaxConnectionPoolSize()));
        alternativeStatementDelimiterTextField.setText(configuration.getAlternativeStatementDelimiter());
    }

    private final EnvironmentConfigLocalListener presentationChangeListener = new EnvironmentConfigLocalListener() {
        @Override
        public void settingsChanged(EnvironmentTypeBundle environmentTypes) {
            EnvironmentType selectedItem = getSelection(environmentTypesComboBox);
            EnvironmentTypeId selectedId = selectedItem == null ? EnvironmentType.DEFAULT.getId() : selectedItem.getId();
            selectedItem = environmentTypes.getEnvironmentType(selectedId);

            List<EnvironmentType> newEnvironmentTypes = new ArrayList<>(environmentTypes.getEnvironmentTypes());
            newEnvironmentTypes.add(0, EnvironmentType.DEFAULT);
            initComboBox(environmentTypesComboBox, newEnvironmentTypes);
            setSelection(environmentTypesComboBox, selectedItem);
            notifyPresentationChanges();
        }
    };
}
