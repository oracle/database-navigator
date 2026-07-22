/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionInputDialog;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
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
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.setFormFieldEnabled;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.util.Dialogs.show;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.liquibase.execution.LiquibaseOperation.CALCULATE_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.CLEAR_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.execution.LiquibaseOperation.DROP_ALL;
import static com.dbn.liquibase.execution.LiquibaseOperation.FUTURE_ROLLBACK;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.LIST_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.MARK_NEXT_CHANGESET_RAN;
import static com.dbn.liquibase.execution.LiquibaseOperation.RELEASE_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.execution.LiquibaseOperation.SNAPSHOT_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.TAG_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UNEXPECTED_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_TESTING_ROLLBACK;
import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperationConfirmations.confirmWorkspaceAvailable;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

/** Project-level Liquibase dashboard for selecting a database context and starting operations. */
public class LiquibaseDashboardForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel hyperlinkPanel;
    private JPanel contextPanel;
    private JLabel connectionLabel;
    private JLabel schemaLabel;
    private DBNComboBox<ConnectionHandler> connectionSelector;
    private DBObjectSelector<DBSchema> schemaSelector;
    private JTabbedPane operationsPanel;

    private final List<LiquibaseDashboardOperationForm> operationForms = DisposableContainers.list(this);

    public LiquibaseDashboardForm(@NotNull LiquibaseDashboardDialog parent) {
        super(parent);

        initHintPanel();
        initHyperlinkPanel();
        initContextSelectors();
        initOperationsPanel();
        updateOperationAvailability();
    }

    private void initHintPanel() {
        hintPanel.add(new DBNHintForm(this, TextContent.plain(txt("cfg.liquibase.hint.Operations")), null, true).getComponent(), BorderLayout.CENTER);
    }

    private void initHyperlinkPanel() {
        HyperLinkForm hyperlinkForm = HyperLinkForm.create(
                "",
                txt("cfg.liquibase.link.LiquibaseDocumentation"),
                txt("app.liquibase.url.Dashboard"));
        hyperlinkPanel.add(hyperlinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initContextSelectors() {
        LiquibaseDashboardDialog dialog = ensureParentDialog();
        DBSchema initialSchema = dialog.getInitialSchema();
        ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
        connectionSelector.setValues(connectionManager.getConnections());
        connectionSelector.setSelectedValue(initialSchema == null ? null : initialSchema.getConnection());

        schemaSelector.initialize(this, SCHEMA);
        schemaSelector.withConnectionContext(this::getSelectedConnection);
        schemaSelector.withValueLoader(() -> {
            ConnectionHandler connection = getSelectedConnection();
            return connection == null ? emptyList() : filter(connection.getObjectBundle().getSchemas(), s -> !s.isSystemSchema());
        });
        schemaSelector.withValuePreselector(() -> initialSchema == null ? null : initialSchema.getName());
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
        addCategory("Changelog", GENERATE_CHANGELOG, GENERATE_DIFF_CHANGELOG, VALIDATE_CHANGELOG, COMPARE_SCHEMAS);
        addCategory("Deploy", UPDATE_DATABASE, UPDATE_TESTING_ROLLBACK, ROLLBACK_CHANGESETS, TAG_DATABASE, MARK_NEXT_CHANGESET_RAN);
        addCategory("Inspect", SHOW_CHANGELOG_STATUS, SHOW_CHANGELOG_HISTORY, UNEXPECTED_CHANGESETS, SNAPSHOT_DATABASE);
        addCategory("Maintenance", SYNCHRONIZE_CHANGELOG, RELEASE_LOCKS, LIST_LOCKS, CLEAR_CHECKSUMS, CALCULATE_CHECKSUMS);
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
        LiquibaseDashboardOperationForm form = new LiquibaseDashboardOperationForm(this, operation, () -> executeOperation(operation));
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

        Project project = ensureProject();
        DatabaseLiquibaseManager manager = DatabaseLiquibaseManager.getInstance(project);
        if (!confirmWorkspaceAvailable(project, manager, schema.getConnection(), operation)) return;

        LiquibaseWorkspaceBundle workspaces = manager.getWorkspaces();
        show(() -> new LiquibaseExecutionInputDialog(schema, operation, workspaces),
                whenOk(dialog -> {
                    LiquibaseExecutionInput input = dialog.getExecutionInput();
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
