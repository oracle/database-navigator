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

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.properties.ui.PropertiesEditorForm;
import com.dbn.connection.ConnectionHandlerStatusListener;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

public class ConnectionPropertiesSettingsForm extends ConfigurationEditorForm<ConnectionPropertiesSettings> {
    private JPanel mainPanel;
    private JCheckBox autoCommitCheckBox;
    private JPanel propertiesPanel;

    private final PropertiesEditorForm propertiesEditorForm;

    public ConnectionPropertiesSettingsForm(final ConnectionPropertiesSettings configuration) {
        super(configuration);

        Map<String, String> properties = new HashMap<>(configuration.getProperties());

        propertiesEditorForm = new PropertiesEditorForm(this, properties, true);
        propertiesPanel.add(propertiesEditorForm.getComponent(), BorderLayout.CENTER);


        resetFormChanges();
        registerComponent(mainPanel);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConnectionPropertiesSettings configuration = getConfiguration();
        boolean newAutoCommit = autoCommitCheckBox.isSelected();
        boolean settingsChanged = configuration.isEnableAutoCommit() != newAutoCommit;

        applyFormChanges(configuration);

        Project project = configuration.getProject();
        ConnectionId connectionId = configuration.getConnectionId();
        SettingsChangeNotifier.register(() -> {
            if (settingsChanged) {
                ProjectEvents.notify(project,
                        ConnectionHandlerStatusListener.TOPIC,
                        (listener) -> listener.statusChanged(connectionId));
            }
        });
    }

    @Override
    public void applyFormChanges(ConnectionPropertiesSettings configuration) throws ConfigurationException {
        propertiesEditorForm.getTable().stopCellEditing();
        configuration.setEnableAutoCommit(autoCommitCheckBox.isSelected());
        configuration.setProperties(propertiesEditorForm.getProperties());
    }

    @Override
    public void resetFormChanges() {
        ConnectionPropertiesSettings configuration = getConfiguration();
        autoCommitCheckBox.setSelected(configuration.isEnableAutoCommit());
        propertiesEditorForm.setProperties(configuration.getProperties());
    }
}
