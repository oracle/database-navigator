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

package com.dbn.ddl.options.ui;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.message.MessageType;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.ddl.options.DDLFileGeneralSettings;
import com.dbn.ddl.options.listener.DDLFileSettingsChangeListener;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import static com.dbn.common.text.TextContent.plain;

public class DDLFileGeneralSettingsForm extends ConfigurationEditorForm<DDLFileGeneralSettings> {
    private JPanel mainPanel;
    private JCheckBox lookupDDLFilesCheckBox;
    private JCheckBox createDDLFileCheckBox;
    private JCheckBox synchronizeDDLFilesCheckBox;
    private JCheckBox useQualifiedObjectNamesCheckBox;
    private JCheckBox makeScriptsRerunnableCheckBox;
    private JPanel hintPanel;

    public DDLFileGeneralSettingsForm(DDLFileGeneralSettings settings) {
        super(settings);

        TextContent hintText = plain("NOTE: When \"Synchronize\" option is enabled, the DDL file content gets overwritten with the source from the underlying database object whenever this gets saved to database.");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, MessageType.INFO, false);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        resetFormChanges();

        boolean synchronizeSelected = synchronizeDDLFilesCheckBox.isSelected();
        useQualifiedObjectNamesCheckBox.setEnabled(synchronizeSelected);
        makeScriptsRerunnableCheckBox.setEnabled(synchronizeSelected);
        hintPanel.setVisible(synchronizeSelected);

        registerComponent(mainPanel);
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            getConfiguration().setModified(true);
            if (e.getSource() == synchronizeDDLFilesCheckBox) {
                boolean synchronizeSelected = synchronizeDDLFilesCheckBox.isSelected();
                useQualifiedObjectNamesCheckBox.setEnabled(synchronizeSelected);
                makeScriptsRerunnableCheckBox.setEnabled(synchronizeSelected);
                hintPanel.setVisible(synchronizeSelected);

            }
        };
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DDLFileGeneralSettings configuration = getConfiguration();
        configuration.getDdlFilesLookupEnabled().to(lookupDDLFilesCheckBox);
        configuration.getDdlFilesCreationEnabled().to(createDDLFileCheckBox);

        boolean settingChanged = configuration.getDdlFilesSynchronizationEnabled().to(synchronizeDDLFilesCheckBox);
        configuration.getUseQualifiedObjectNames().to(useQualifiedObjectNamesCheckBox);
        configuration.getMakeScriptsRerunnable().to(makeScriptsRerunnableCheckBox);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (settingChanged) {
                ProjectEvents.notify(project,
                        DDLFileSettingsChangeListener.TOPIC,
                        (listener) -> listener.settingsChanged(project));

            }
        });
    }

    @Override
    public void resetFormChanges() {
        DDLFileGeneralSettings settings = getConfiguration();
        settings.getDdlFilesLookupEnabled().from(lookupDDLFilesCheckBox);
        settings.getDdlFilesCreationEnabled().from(createDDLFileCheckBox);
        settings.getDdlFilesSynchronizationEnabled().from(synchronizeDDLFilesCheckBox);
        settings.getUseQualifiedObjectNames().from(useQualifiedObjectNamesCheckBox);
        settings.getMakeScriptsRerunnable().from(makeScriptsRerunnableCheckBox);
    }
}
