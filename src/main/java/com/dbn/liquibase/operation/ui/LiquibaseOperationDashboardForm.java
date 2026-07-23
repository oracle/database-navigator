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

package com.dbn.liquibase.operation.ui;

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
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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
import static com.dbn.liquibase.operation.LiquibaseOperation.CALCULATE_CHECKSUMS;
import static com.dbn.liquibase.operation.LiquibaseOperation.CLEAR_CHECKSUMS;
import static com.dbn.liquibase.operation.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.operation.LiquibaseOperation.DROP_ALL;
import static com.dbn.liquibase.operation.LiquibaseOperation.FUTURE_ROLLBACK;
import static com.dbn.liquibase.operation.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.operation.LiquibaseOperation.GENERATE_DATABASE_DOCUMENTATION;
import static com.dbn.liquibase.operation.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.operation.LiquibaseOperation.LIST_LOCKS;
import static com.dbn.liquibase.operation.LiquibaseOperation.MARK_NEXT_CHANGESET_RAN;
import static com.dbn.liquibase.operation.LiquibaseOperation.RELEASE_LOCKS;
import static com.dbn.liquibase.operation.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.operation.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.operation.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.operation.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.operation.LiquibaseOperation.SNAPSHOT_DATABASE;
import static com.dbn.liquibase.operation.LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
import static com.dbn.liquibase.operation.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_SQL;
import static com.dbn.liquibase.operation.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_TO_TAG;
import static com.dbn.liquibase.operation.LiquibaseOperation.TAG_DATABASE;
import static com.dbn.liquibase.operation.LiquibaseOperation.UNEXPECTED_CHANGESETS;
import static com.dbn.liquibase.operation.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.operation.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.operation.LiquibaseOperation.UPDATE_TESTING_ROLLBACK;
import static com.dbn.liquibase.operation.LiquibaseOperation.VALIDATE_CHANGELOG;
import static com.dbn.liquibase.operation.LiquibaseOperationConfirmations.confirmWorkspaceAvailable;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

/** Project-level Liquibase dashboard for selecting a database context and starting operations. */
public class LiquibaseOperationDashboardForm extends DBNFormBase {
    private static final String STATE_CATEGORY = "LIQUIBASE_DASHBOARD";
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
    private JTabbedPane operationsPanel;

    private final List<LiquibaseDashboardItemForm> operationForms = DisposableContainers.list(this);

    public LiquibaseOperationDashboardForm(@NotNull LiquibaseOperationDashboardDialog parent) {
        super(parent);

        initHintPanel();
        initHyperlinkPanel();
        initContextSelectors();
        initOperationsPanel();
        updateOperationAvailability();
    }

    private void initHintPanel() {
        hintPanel.add(new DBNHintForm(this, TextContent.plain(txt("app.liquibase.hint.OperationDashboard")), null, true).getComponent(), BorderLayout.CENTER);
    }

    private void initHyperlinkPanel() {
        HyperLinkForm hyperlinkForm = HyperLinkForm.create(
                "",
                txt("app.liquibase.link.LiquibaseDocumentation"),
                txt("app.liquibase.url.OperationDashboard"));
        hyperlinkPanel.add(hyperlinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initContextSelectors() {
        LiquibaseOperationDashboardDialog dialog = ensureParentDialog();
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
            updateOperationAvailability();
        });
        onSelectionChange(schemaSelector, schema -> updateOperationAvailability());
        updateOperationAvailability();
    }

    private void initOperationsPanel() {
        addCategory("Changelog", GENERATE_CHANGELOG, GENERATE_DIFF_CHANGELOG, VALIDATE_CHANGELOG);
        addCategory("Deploy", UPDATE_DATABASE, ROLLBACK_CHANGESETS, TAG_DATABASE, UPDATE_TESTING_ROLLBACK);
        addCategory("Inspect", SHOW_CHANGELOG_STATUS, SHOW_CHANGELOG_HISTORY, UNEXPECTED_CHANGESETS, COMPARE_SCHEMAS, SNAPSHOT_DATABASE, GENERATE_DATABASE_DOCUMENTATION);
        addCategory("Maintenance", SYNCHRONIZE_CHANGELOG, SYNCHRONIZE_CHANGELOG_TO_TAG, LIST_LOCKS, RELEASE_LOCKS, CALCULATE_CHECKSUMS, CLEAR_CHECKSUMS, MARK_NEXT_CHANGESET_RAN);
        addCategory("PreviewSql", UPDATE_SQL, ROLLBACK_SQL, FUTURE_ROLLBACK, SYNCHRONIZE_CHANGELOG_SQL);
        addCategory("More", DROP_ALL);
    }

    private void addCategory(@NotNull @NonNls String key, @NotNull LiquibaseOperation... operations) {
        JPanel itemsPanel = new JPanel();
        verticalBoxLayout(itemsPanel);
        for (LiquibaseOperation operation : operations) addOperation(itemsPanel, operation);
        itemsPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        operationsPanel.addTab(txt("app.liquibase.group." + key), scrollPane);
    }

    private void addOperation(@NotNull JPanel parent, @NotNull LiquibaseOperation operation) {
        LiquibaseDashboardItemForm form = new LiquibaseDashboardItemForm(this, operation, () -> executeOperation(operation));
        parent.add(form.getComponent());
        operationForms.add(form);
    }

    private void updateOperationAvailability() {
        boolean available = getSelectedSchema() != null;
        operationForms.forEach(form -> form.setOperationAvailable(available));
    }

    private void executeOperation(@NotNull LiquibaseOperation operation) {
        DBSchema schema = getSelectedSchema();
        if (schema == null) return;

        ConnectionHandler connection = schema.getConnection();
        if (!confirmWorkspaceAvailable(connection, operation.getSupport())) return;

        Project project = ensureProject();
        DatabaseLiquibaseManager manager = DatabaseLiquibaseManager.getInstance(project);
        LiquibaseWorkspaceBundle workspaces = manager.getWorkspaces();
        show(() -> new LiquibaseOperationInputDialog(schema, operation, workspaces),
                whenOk(dialog -> {
                    LiquibaseOperationInput input = dialog.getExecutionInput();
                    manager.executeOperation(input, null);
                }));
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
