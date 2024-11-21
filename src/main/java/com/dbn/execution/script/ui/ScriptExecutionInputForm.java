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

package com.dbn.execution.script.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.PresentableFactory;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.SchemaId;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.common.ui.ExecutionTimeoutForm;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.ScriptExecutionInput;
import com.dbn.execution.script.ScriptExecutionManager;
import com.dbn.vfs.DBVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;

public class ScriptExecutionInputForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private DBNComboBox<CmdLineInterface> cmdLineExecutableComboBox;
    private JCheckBox clearOutputCheckBox;
    private JPanel hintPanel;
    private JPanel executionTimeoutPanel;

    private final DBNHeaderForm headerForm;
    private final ExecutionTimeoutForm executionTimeoutForm;

    ScriptExecutionInputForm(@NotNull ScriptExecutionInputDialog parent, @NotNull ScriptExecutionInput executionInput) {
        super(parent);

        VirtualFile sourceFile = executionInput.getSourceFile();
        String headerTitle = sourceFile.isInLocalFileSystem() ? sourceFile.getPath() : sourceFile.getName();
        Icon headerIcon = sourceFile.getFileType().getIcon();
        if (sourceFile instanceof DBVirtualFile) {
            DBVirtualFile databaseVirtualFile = (DBVirtualFile) sourceFile;
            headerIcon = databaseVirtualFile.getIcon();
        }

        headerForm = new DBNHeaderForm(this, headerTitle, headerIcon);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        TextContent hintText = plain(
                "Script execution uses the Command-Line Interface executable supplied with your database client. " +
                "Make sure it is available in the \"PATH\" environment variable or provide the path to the executable.");

        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        Project project = ensureProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        connectionComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
        connectionComboBox.setEnabled(sourceFile.isInLocalFileSystem());
        connectionComboBox.setValues(connectionManager.getConnections());

        schemaComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);

        cmdLineExecutableComboBox.set(ValueSelectorOption.HIDE_ICON, true);
        cmdLineExecutableComboBox.setValueFactory(new PresentableFactory<>("New Cmd-Line Interface...") {
            @Override
            public void create(Consumer<CmdLineInterface> consumer) {
                ConnectionHandler connection = connectionComboBox.getSelectedValue();
                if (connection != null) {
                    ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(project);
                    scriptExecutionManager.createCmdLineInterface(connection.getDatabaseType(), null, consumer);
                }
            }
        });

        clearOutputCheckBox.setSelected(executionInput.isClearOutput());
        executionTimeoutForm = new ExecutionTimeoutForm(this, executionInput, DBDebuggerType.NONE) {
            @Override
            protected void handleChange(boolean hasError) {
                updateButtons();
            }
        };
        executionTimeoutPanel.add(executionTimeoutForm.getComponent());

        updateControls(executionInput);
        clearOutputCheckBox.addActionListener(e -> {
            boolean selected = clearOutputCheckBox.isSelected();
            executionInput.setClearOutput(selected);
        });

        connectionComboBox.addListener((oldValue, newValue) -> {
            executionInput.setTargetConnection(newValue);
            updateControls(executionInput);
        });

        schemaComboBox.addListener((oldValue, newValue) -> {
            executionInput.setTargetSchemaId(newValue);
            updateButtons();
        });

        cmdLineExecutableComboBox.addListener((oldValue, newValue) -> {
            executionInput.setCmdLineInterface(newValue);
            updateButtons();
        });
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return connectionComboBox.isEnabled() ? connectionComboBox : schemaComboBox;
    }

    private void updateControls(ScriptExecutionInput executionInput) {
        ConnectionHandler connection = executionInput.getConnection();
        SchemaId schema = executionInput.getSchemaId();
        CmdLineInterface cmdLineInterface;
        if (isLiveConnection(connection)) {
            schema = Commons.nvln(schema, connection.getDefaultSchema());
            connectionComboBox.setSelectedValue(connection);
            schemaComboBox.setValues(connection.getSchemaIds());
            schemaComboBox.setSelectedValue(schema);
            schemaComboBox.setEnabled(true);
            headerForm.setBackground(connection.getEnvironmentType().getColor());

            DatabaseType databaseType = connection.getDatabaseType();
            ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(ensureProject());
            List<CmdLineInterface> interfaces = scriptExecutionManager.getAvailableInterfaces(databaseType);
            cmdLineExecutableComboBox.clearValues();
            cmdLineExecutableComboBox.addValues(interfaces);
            cmdLineExecutableComboBox.setEnabled(true);

            cmdLineInterface = scriptExecutionManager.getRecentInterface(databaseType);
            if (cmdLineInterface != null) {
                cmdLineExecutableComboBox.setSelectedValue(cmdLineInterface);

            }

            executionInput.setTargetConnection(connection);
            executionInput.setTargetSchemaId(schema);
            executionInput.setCmdLineInterface(cmdLineInterface);
        } else {
            schemaComboBox.setEnabled(false);
            cmdLineExecutableComboBox.setEnabled(false);
        }
        updateButtons();
    }

    private void updateButtons() {
        ScriptExecutionInputDialog parentComponent = ensureParentComponent();
        parentComponent.setActionEnabled(
                connectionComboBox.getSelectedValue() != null &&
                schemaComboBox.getSelectedValue() != null &&
                cmdLineExecutableComboBox.getSelectedValue() != null &&
                !executionTimeoutForm.hasErrors());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
