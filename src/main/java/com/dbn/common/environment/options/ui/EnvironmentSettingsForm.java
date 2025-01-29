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

package com.dbn.common.environment.options.ui;

import com.dbn.common.action.BasicActionButton;
import com.dbn.common.environment.EnvironmentTypeBundle;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableCellEditor;

import static com.dbn.common.ui.util.ClientProperty.COMPONENT_GROUP_QUALIFIER;
import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class EnvironmentSettingsForm extends ConfigurationEditorForm<EnvironmentSettings> {
    private JPanel mainPanel;
    private JCheckBox connectionTabsCheckBox;
    private JCheckBox objectEditorTabsCheckBox;
    private JCheckBox scriptEditorTabsCheckBox;
    private JCheckBox dialogHeadersCheckBox;
    private JCheckBox executionResultTabsCheckBox;
    private JPanel environmentTypesPanel;
    private JPanel environmentApplicabilityPanel;
    private JPanel environmentTypesTablePanel;
    private JLabel environmentColorsLabel;
    private EnvironmentTypesEditorTable environmentTypesTable;

    public EnvironmentSettingsForm(EnvironmentSettings settings) {
        super(settings);
        environmentTypesTable = new EnvironmentTypesEditorTable(this, settings.getEnvironmentTypes());
        environmentTypesTablePanel.add(initTableComponent());

        EnvironmentVisibilitySettings visibilitySettings = settings.getVisibilitySettings();
        visibilitySettings.getConnectionTabs().from(connectionTabsCheckBox);
        visibilitySettings.getObjectEditorTabs().from(objectEditorTabsCheckBox);
        visibilitySettings.getScriptEditorTabs().from(scriptEditorTabsCheckBox);
        visibilitySettings.getDialogHeaders().from(dialogHeadersCheckBox);
        visibilitySettings.getExecutionResultTabs().from(executionResultTabsCheckBox);


        registerComponents(mainPanel);
    }

    private JPanel initTableComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(environmentTypesTable);
        decorator.setAddAction(b -> environmentTypesTable.insertRow());
        decorator.setRemoveAction(b -> environmentTypesTable.removeRow());
        decorator.setMoveUpAction(b -> environmentTypesTable.moveRowUp());
        decorator.setMoveDownAction(b -> environmentTypesTable.moveRowDown());
        decorator.addExtraAction(new BasicActionButton("Revert Changes", null, Icons.ACTION_REVERT) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TableCellEditor cellEditor = environmentTypesTable.getCellEditor();
                if (cellEditor != null) {
                    cellEditor.cancelCellEditing();
                }
                environmentTypesTable.setEnvironmentTypes(EnvironmentTypeBundle.DEFAULT);
            }
        });

        return createToolbarDecoratorComponent(decorator, environmentTypesTable);
    }

    @Override
    protected void initAccessibility() {
        COMPONENT_GROUP_QUALIFIER.set(environmentColorsLabel, true);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
    
    @Override
    public void applyFormChanges() throws ConfigurationException {
        EnvironmentSettings configuration = getConfiguration();
        EnvironmentTypesTableModel model = environmentTypesTable.getModel();
        model.validate();
        EnvironmentTypeBundle environmentTypeBundle = model.getEnvironmentTypes();
        boolean settingsChanged = configuration.setEnvironmentTypes(environmentTypeBundle);

        EnvironmentVisibilitySettings visibilitySettings = configuration.getVisibilitySettings();
        boolean visibilityChanged =
            visibilitySettings.getConnectionTabs().to(connectionTabsCheckBox) ||
            visibilitySettings.getObjectEditorTabs().to(objectEditorTabsCheckBox) ||
            visibilitySettings.getScriptEditorTabs().to(scriptEditorTabsCheckBox)||
            visibilitySettings.getDialogHeaders().to(dialogHeadersCheckBox)||
            visibilitySettings.getExecutionResultTabs().to(executionResultTabsCheckBox);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (settingsChanged || visibilityChanged) {
                ProjectEvents.notify(project,
                        EnvironmentManagerListener.TOPIC,
                        (listener) -> listener.configurationChanged(project));
            }
        });
    }

    @Override
    public void resetFormChanges() {
        EnvironmentSettings settings = getConfiguration();
        environmentTypesTable.getModel().setEnvironmentTypes(settings.getEnvironmentTypes());

        EnvironmentVisibilitySettings visibilitySettings = settings.getVisibilitySettings();
        visibilitySettings.getConnectionTabs().from(connectionTabsCheckBox);
        visibilitySettings.getObjectEditorTabs().from(objectEditorTabsCheckBox);
        visibilitySettings.getScriptEditorTabs().from(scriptEditorTabsCheckBox);
        visibilitySettings.getDialogHeaders().from(dialogHeadersCheckBox);
        visibilitySettings.getExecutionResultTabs().from(executionResultTabsCheckBox);
    }
}
