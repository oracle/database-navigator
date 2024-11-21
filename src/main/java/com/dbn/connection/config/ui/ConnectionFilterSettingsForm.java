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

import com.dbn.browser.options.ObjectFilterChangeListener;
import com.dbn.common.constant.Constant;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionFilterSettings;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class ConnectionFilterSettingsForm extends CompositeConfigurationEditorForm<ConnectionFilterSettings> {
    private JPanel mainPanel;
    private JPanel objectTypesFilterPanel;
    private JPanel objectCustomFiltersPanel;
    private JCheckBox hideEmptySchemasCheckBox;
    private JCheckBox hideAuditColumnsCheckBox;
    private JCheckBox hidePseudoColumnsCheckBox;

    public ConnectionFilterSettingsForm(ConnectionFilterSettings settings) {
        super(settings);
        objectCustomFiltersPanel.add(settings.getObjectFilterSettings().createComponent(), BorderLayout.CENTER);
        objectTypesFilterPanel.add(settings.getObjectTypeFilterSettings().createComponent(), BorderLayout.CENTER);

        hideEmptySchemasCheckBox.setSelected(settings.isHideEmptySchemas());
        hideAuditColumnsCheckBox.setSelected(settings.isHideAuditColumns());
        hidePseudoColumnsCheckBox.setSelected(settings.isHidePseudoColumns());

        registerComponent(hideEmptySchemasCheckBox);
        registerComponent(hideAuditColumnsCheckBox);
        registerComponent(hidePseudoColumnsCheckBox);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConnectionFilterSettings configuration = getConfiguration();
        boolean notifyFilterListenersSchemas = configuration.isHideEmptySchemas() != hideEmptySchemasCheckBox.isSelected();
        boolean notifyFilterListenersColumns =
                configuration.isHideAuditColumns() != hideAuditColumnsCheckBox.isSelected() ||
                configuration.isHidePseudoColumns() != hidePseudoColumnsCheckBox.isSelected();

        applyFormChanges(configuration);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            ConnectionId connectionId = configuration.getConnectionId();
            if (notifyFilterListenersSchemas) {
                ProjectEvents.notify(project,
                        ObjectFilterChangeListener.TOPIC,
                        (listener) -> listener.nameFiltersChanged(connectionId, Constant.array(DBObjectType.SCHEMA)));
            }
            if (notifyFilterListenersColumns) {
                ProjectEvents.notify(project,
                    ObjectFilterChangeListener.TOPIC,
                    (listener) -> listener.nameFiltersChanged(connectionId, Constant.array(DBObjectType.COLUMN)));
            }
        });
    }

    @Override
    public void applyFormChanges(ConnectionFilterSettings configuration) throws ConfigurationException {
        configuration.setHideEmptySchemas(hideEmptySchemasCheckBox.isSelected());
        configuration.setHideAuditColumns(hideAuditColumnsCheckBox.isSelected());
        configuration.setHidePseudoColumns(hidePseudoColumnsCheckBox.isSelected());
    }
}
