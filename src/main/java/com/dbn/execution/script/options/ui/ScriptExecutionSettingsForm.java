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

package com.dbn.execution.script.options.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.common.ui.util.Popups;
import com.dbn.connection.DatabaseType;
import com.dbn.execution.script.CmdLineInterfaceBundle;
import com.dbn.execution.script.ScriptExecutionManager;
import com.dbn.execution.script.options.ScriptExecutionSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;
import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class ScriptExecutionSettingsForm extends ConfigurationEditorForm<ScriptExecutionSettings> {
    private JPanel mainPanel;
    private JPanel cmdLineInterfacesTablePanel;
    private JTextField executionTimeoutTextField;
    private JLabel cmdLineInterfaceLabel;
    private final CmdLineInterfacesTable cmdLineInterfacesTable;

    public ScriptExecutionSettingsForm(ScriptExecutionSettings settings) {
        super(settings);
        cmdLineInterfacesTable = new CmdLineInterfacesTable(this, settings.getCommandLineInterfaces());

        cmdLineInterfacesTablePanel.add(initTableComponent());

        cmdLineInterfacesTable.getParent().setBackground(cmdLineInterfacesTable.getBackground());
        executionTimeoutTextField.setText(String.valueOf(settings.getExecutionTimeout()));
        cmdLineInterfaceLabel.setLabelFor(cmdLineInterfacesTable);
        registerComponents(mainPanel);
    }

    private JPanel initTableComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(cmdLineInterfacesTable);
        decorator.setAddAction(b -> showNewInterfacePopup(b.getDataContext(), b.getPreferredPopupPoint()));
        decorator.setRemoveAction(b -> cmdLineInterfacesTable.removeRow());
        decorator.setMoveUpAction(b -> cmdLineInterfacesTable.moveRowUp());
        decorator.setMoveDownAction(b -> cmdLineInterfacesTable.moveRowDown());
        decorator.setPreferredSize(new Dimension(-1, 300));

        return createToolbarDecoratorComponent(decorator, cmdLineInterfacesTable);
    }

    @Override
    protected void initAccessibility() {
        setAccessibleUnit(executionTimeoutTextField, txt("app.shared.unit.Seconds"), txt("app.shared.hint.ZeroForNoTimeout"));
    }

    private void showNewInterfacePopup(DataContext dataContext, RelativePoint point) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (DatabaseType databaseType : DatabaseType.nativelySupported()) {
            actionGroup.add(new CreateInterfaceAction(databaseType));
        }

        ListPopup popup = Popups.popupBuilder(actionGroup, dataContext)
                .withTitle("Database Type")
                .withTitleVisible(false)
                .build();

        popup.show(point);
    }

    public class CreateInterfaceAction extends BasicAction {
        private final DatabaseType databaseType;
        CreateInterfaceAction(DatabaseType databaseType) {
            super();
            getTemplatePresentation().setText(databaseType.getName(), false);
            getTemplatePresentation().setIcon(databaseType.getIcon());
            this.databaseType = databaseType;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = e.getProject();
            if (project != null) {
                ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(project);
                scriptExecutionManager.createCmdLineInterface(
                        databaseType,
                        cmdLineInterfacesTable.getNames(),
                        inputValue -> cmdLineInterfacesTable.addInterface(inputValue));
            }
        }
    }
    
    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
    
    @Override
    public void applyFormChanges() throws ConfigurationException {
        ScriptExecutionSettings configuration = getConfiguration();
        int executionTimeout = ConfigurationEditors.validateIntegerValue(executionTimeoutTextField, "Execution timeout", true, 0, 6000, "\nUse value 0 for no timeout");
        CmdLineInterfacesTableModel model = cmdLineInterfacesTable.getModel();
        model.validate();
        CmdLineInterfaceBundle executorBundle = model.getBundle();
        configuration.setCommandLineInterfaces(executorBundle);
        configuration.setExecutionTimeout(executionTimeout);
    }

    @Override
    public void resetFormChanges() {
        ScriptExecutionSettings settings = getConfiguration();
        executionTimeoutTextField.setText(Integer.toString(settings.getExecutionTimeout()));
        cmdLineInterfacesTable.getModel().setBundle(settings.getCommandLineInterfaces());
    }
}
