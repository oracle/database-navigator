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

package com.dbn.liquibase.workflow.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.operation.ui.LiquibaseDashboardItemForm;
import com.dbn.liquibase.workflow.LiquibaseWorkflow;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.setFormFieldEnabled;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.util.Dialogs.show;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.liquibase.operation.LiquibaseOperationConfirmations.confirmWorkspaceAvailable;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

/** Project-level Liquibase dashboard for selecting and starting workflows. */
public class LiquibaseWorkflowDashboardForm extends DBNFormBase {
    private static final String STATE_CATEGORY = "LIQUIBASE_WORKFLOW_DASHBOARD";
    private static final String ATTR_CONNECTION = "connection-selection";
    private static final String ATTR_SCHEMA = "schema-selection";

    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel hyperlinkPanel;
    private JPanel contextPanel;
    private JLabel connectionLabel;
    private JLabel schemaLabel;
    private DBNComboBox<ConnectionHandler> connectionSelector;
    private DBObjectSelector<DBSchema> schemaSelector;
    private JScrollPane workflowsScrollPane;

    private final List<LiquibaseDashboardItemForm> workflowForms = DisposableContainers.list(this);

    public LiquibaseWorkflowDashboardForm(@NotNull LiquibaseWorkflowDashboardDialog parent) {
        super(parent);

        initHintPanel();
        initHyperlinkPanel();
        initContextSelectors();
        initWorkflowsPanel();
        updateWorkflowAvailability();
    }

    private void initHintPanel() {
        hintPanel.add(new DBNHintForm(this, TextContent.plain(txt("app.liquibase.hint.Workflows")), null, true).getComponent(), BorderLayout.CENTER);
    }

    private void initHyperlinkPanel() {
        HyperLinkForm hyperlinkForm = HyperLinkForm.create(
                "",
                txt("app.liquibase.link.LiquibaseDocumentation"),
                txt("app.liquibase.url.OperationDashboard"));
        hyperlinkPanel.add(hyperlinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initContextSelectors() {
        LiquibaseWorkflowDashboardDialog dialog = ensureParentDialog();
        DBSchema initialSchema = dialog.getInitialSchema();
        ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(ensureProject());
        StateAttributes state = liquibaseManager.getState(STATE_CATEGORY);

        if (initialSchema != null) {
            state.setAttribute(ATTR_CONNECTION, initialSchema.getConnection().getName());
            state.setAttribute(ATTR_SCHEMA, initialSchema.getName());
        }

        connectionSelector.setValues(connectionManager.getConnections());
        initPersistence(connectionSelector, state, ATTR_CONNECTION);

        schemaSelector.initialize(this, SCHEMA);
        schemaSelector.withConnectionContext(this::getSelectedConnection);
        schemaSelector.withValueLoader(() -> {
            ConnectionHandler connection = getSelectedConnection();
            return connection == null ? emptyList() : filter(connection.getObjectBundle().getSchemas(), s -> !s.isSystemSchema());
        });
        initPersistence(schemaSelector, state, ATTR_SCHEMA);
        schemaSelector.triggerLoad();
        setFormFieldEnabled(schemaSelector, "CONTEXT_AVAILABILITY", getSelectedConnection() != null);

        onSelectionChange(connectionSelector, connection -> {
            schemaSelector.reloadValues();
            setFormFieldEnabled(schemaSelector, "CONTEXT_AVAILABILITY", connection != null);
            updateWorkflowAvailability();
        });
        onSelectionChange(schemaSelector, schema -> updateWorkflowAvailability());
    }

    private void initWorkflowsPanel() {
        JPanel itemsPanel = new JPanel();
        verticalBoxLayout(itemsPanel);
        for (LiquibaseWorkflow workflow : LiquibaseWorkflow.values()) {
            LiquibaseDashboardItemForm form = new LiquibaseDashboardItemForm(
                    this,
                    workflow,
                    () -> executeWorkflow(workflow));
            itemsPanel.add(form.getComponent());
            workflowForms.add(form);
        }
        itemsPanel.add(Box.createVerticalGlue());
        workflowsScrollPane.setViewportView(itemsPanel);
        workflowsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        workflowsScrollPane.setBorder(null);
    }

    private void updateWorkflowAvailability() {
        boolean available = getSelectedSchema() != null;
        workflowForms.forEach(form -> form.setOperationAvailable(available));
    }

    private void executeWorkflow(@NotNull LiquibaseWorkflow workflow) {
        DBSchema schema = getSelectedSchema();
        if (schema == null) return;
        if (!confirmWorkspaceAvailable(schema.getConnection(), workflow.getSupport())) return;

        Project project = ensureProject();
        show(() -> new LiquibaseWorkflowInputDialog(schema, workflow),
                whenOk(dialog -> DatabaseLiquibaseManager.getInstance(project).executeWorkflow(dialog.getWorkflowInput(), null)));
    }

    private ConnectionHandler getSelectedConnection() {
        return getSelection(connectionSelector);
    }

    private DBSchema getSelectedSchema() {
        return schemaSelector.getSelectedValue();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return connectionSelector;
    }
}
