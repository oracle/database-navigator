/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.execution.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;

public class LiquibaseExecutionInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JLabel schemaLabel;
    private DBObjectSelector<DBSchema> schemaSelector;
    private DBNComboBox<LiquibaseWorkspace> workspaceSelector;
    private JLabel targetConnectionLabel;
    private DBNComboBox<ConnectionHandler> targetConnectionSelector;
    private JLabel targetSchemaLabel;
    private DBObjectSelector<DBSchema> targetSchemaSelector;

    private final LiquibaseExecutionInputDialog parent;
    private final LiquibaseExecutionInput executionInput;

    LiquibaseExecutionInputForm(@NotNull LiquibaseExecutionInputDialog parent) {
        super(parent);
        this.parent = parent;
        this.executionInput = parent.getExecutionInput();

        initHeaderPanel();
        initHintPanel();
        initSchemaSelector();
        initWorkspaceSelector();
        initTargetConnectionSelector();
        initTargetSchemaSelector();
        executionInput.setTargetSchema(getTargetSchema());
        executionInput.setWorkspace(workspaceSelector.getSelectedValue());
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = executionInput.getSourceConnection();
        headerPanel.add(new DBNHeaderForm(this, connection).getComponent());
    }

    private void initHintPanel() {
        LiquibaseOperation operation = executionInput.getOperation();
        TextContent hint = plain(operation.getHint());
        hintPanel.add(new DBNHintForm(this, hint, null, true).getComponent());
    }

    private void initSchemaSelector() {
        ConnectionHandler connection = executionInput.getSourceConnection();
        schemaSelector
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> connection)
                .withValueLoader(() -> connection.getObjectBundle().getSchemas())
                .withValuePreselector(() -> executionInput.getSourceSchema().getName())
                .triggerLoad();
        schemaSelector.setEnabled(false);
    }

    private void initWorkspaceSelector() {
        LiquibaseWorkspaceBundle workspaces = parent.getWorkspaces();
        DBSchema sourceSchema = executionInput.getSourceSchema();

        LiquibaseWorkspace selectedWorkspace = workspaces.getSelectedWorkspace(
                sourceSchema.getConnectionId(),
                sourceSchema.getSchemaId());
        workspaceSelector.setValues(workspaces.getWorkspaceList());
        workspaceSelector.setSelectedValue(selectedWorkspace);
        workspaceSelector.withValueFactory(new ValueFactory<>(txt("app.liquibase.action.NewWorkspace")) {
            @Override
            public void createValue(Consumer<LiquibaseWorkspace> consumer) {
                DatabaseLiquibaseManager.getInstance(executionInput.getProject())
                        .openWorkspaceCreationDialog(consumer);
            }
        });
        onSelectionChange(workspaceSelector, value -> markFormChanged());
    }

    private void initTargetSchemaSelector() {
        targetSchemaSelector
                .initialize(this, SCHEMA)
                .withConnectionContext(this::getTargetConnection)
                .withValueLoader(() -> {
                    ConnectionHandler connection = getTargetConnection();
                    return connection == null ? java.util.Collections.emptyList() : connection.getObjectBundle().getSchemas();
                })
                .triggerLoad();
    }

    private void initTargetConnectionSelector() {
        ConnectionHandler sourceConnection = executionInput.getSourceConnection();

        Project project = executionInput.getProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        targetConnectionSelector.setValues(connectionManager.getConnections());
        targetConnectionSelector.setSelectedValue(sourceConnection);
        onSelectionChange(targetConnectionSelector, value -> {
            targetSchemaSelector.reloadValues();
            markFormChanged();
        });
    }

    @Override
    protected void initValidation() {
        LiquibaseOperation operation = executionInput.getOperation();
        addSelectionValidation(workspaceSelector, txt("msg.liquibase.error.WorkspaceRequired"));
        addValidation(targetConnectionSelector,
                selector -> operation != COMPARE || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetConnection"));
        addValidation(targetSchemaSelector,
                selector -> operation != COMPARE || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetSchema"));
    }

    @Override
    protected void initFieldAvailability() {
        LiquibaseOperation operation = executionInput.getOperation();
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> false, array(schemaSelector));
        fieldAdapter.initFieldsVisibility(() -> operation == COMPARE, array(
                targetConnectionLabel,
                targetConnectionSelector,
                targetSchemaLabel,
                targetSchemaSelector));
    }


    @NotNull
    public LiquibaseExecutionInput getExecutionInput() {
        return executionInput;
    }

    @Override
    public void applyFormChanges() {
        executionInput.setWorkspace(getSelection(workspaceSelector));
        executionInput.setTargetSchema(getTargetSchema());

        executionInput.getWorkspaces().rememberWorkspace(
                executionInput.getSourceConnection().getConnectionId(),
                executionInput.getSourceSchema().getSchemaId(),
                executionInput.getWorkspace());
    }

    @Nullable
    public DBSchema getTargetSchema() {
        return getSelection(targetSchemaSelector);
    }

    @Nullable
    private ConnectionHandler getTargetConnection() {
        return getSelection(targetConnectionSelector);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return workspaceSelector;
    }
}
