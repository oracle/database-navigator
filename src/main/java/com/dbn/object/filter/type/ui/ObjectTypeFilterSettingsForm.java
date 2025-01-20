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

package com.dbn.object.filter.type.ui;

import com.dbn.browser.options.ObjectFilterChangeListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionId;
import com.dbn.object.filter.type.ObjectTypeFilterSetting;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Color;

public class ObjectTypeFilterSettingsForm extends ConfigurationEditorForm<ObjectTypeFilterSettings> {
    private JPanel mainPanel;
    private JScrollPane visibleObjectsScrollPane;
    private JCheckBox useMasterSettingsCheckBox;
    private JLabel visibleObjectTypesLabel;
    private CheckBoxList<ObjectTypeFilterSetting> visibleObjectsList;

    public ObjectTypeFilterSettingsForm(ObjectTypeFilterSettings configuration) {
        super(configuration);

        boolean masterSettingsAvailable = configuration.getMasterSettings() != null;
        useMasterSettingsCheckBox.setVisible(masterSettingsAvailable);
        if (masterSettingsAvailable) {
            visibleObjectTypesLabel.setVisible(false);
            useMasterSettingsCheckBox.addActionListener(e -> {
                boolean enabled = !useMasterSettingsCheckBox.isSelected();
                visibleObjectsList.setEnabled(enabled);
                Color background = enabled ? UIUtil.getListBackground() : UIUtil.getComboBoxDisabledBackground();
                visibleObjectsList.setBackground(background);
                visibleObjectsList.clearSelection();
                visibleObjectsScrollPane.setVisible(enabled);
                UserInterface.repaint(mainPanel);
            });
        } else {
            mainPanel.setBorder(null);
        }
        configuration.getUseMasterSettings().from(useMasterSettingsCheckBox);
        boolean enabled = !masterSettingsAvailable || !useMasterSettingsCheckBox.isSelected();
        visibleObjectsList.setEnabled(enabled);
        visibleObjectsList.setBackground(enabled ? UIUtil.getListBackground() : UIUtil.getComboBoxDisabledBackground());
        visibleObjectsList.setElements(configuration.getSettings());
        visibleObjectsScrollPane.setVisible(enabled);

        registerComponents(visibleObjectsList, useMasterSettingsCheckBox);
    }


    public boolean isSelected(ObjectTypeFilterSetting objectFilterEntry) {
        return visibleObjectsList.isSelected(objectFilterEntry);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ObjectTypeFilterSettings configuration = getConfiguration();
        boolean notifyFilterListeners = configuration.isModified();
        visibleObjectsList.applyChanges();
        configuration.getUseMasterSettings().to(useMasterSettingsCheckBox);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
             if (notifyFilterListeners) {
                 ConnectionId connectionId = configuration.getConnectionId();
                 ProjectEvents.notify(project,
                         ObjectFilterChangeListener.TOPIC,
                         (listener) -> listener.typeFiltersChanged(connectionId));
             }
         });
    }

    @Override
    public void resetFormChanges() {}
}
