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

import com.dbn.common.color.Colors;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.util.Fonts;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseOperationSupport;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionInputDialog;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.util.Dialogs.show;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.common.util.Messages.showInfoDialog;
import static com.dbn.liquibase.execution.LiquibaseOperation.CLEAR_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.execution.LiquibaseOperation.DROP_ALL;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.MARK_NEXT_CHANGESET_RAN;
import static com.dbn.liquibase.execution.LiquibaseOperation.RELEASE_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.TAG_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/** Single-screen overview of the Liquibase operations available for a database schema. */
public class LiquibaseOperationsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JTabbedPane operationsPanel;

    private final DBSchema schema;
    public LiquibaseOperationsForm(@NotNull LiquibaseOperationsDialog parent) {
        super(parent);
        schema = parent.getSchema();

        initHeaderPanel();
        initHintPanel();
        initOperationsPanel();
    }

    private void initHeaderPanel() {
        headerPanel.add(new DBNHeaderForm(this, schema.getConnection()).getComponent(), BorderLayout.CENTER);
    }

    private void initHintPanel() {
        hintPanel.add(new DBNHintForm(this, TextContent.plain(txt("cfg.liquibase.hint.Operations")), null, true).getComponent(), BorderLayout.CENTER);
    }

    private void initOperationsPanel() {
        addCategory("Changelog", GENERATE_CHANGELOG, GENERATE_DIFF_CHANGELOG, VALIDATE_CHANGELOG, COMPARE_SCHEMAS);
        addCategory("Deploy", UPDATE_DATABASE, ROLLBACK_CHANGESETS, TAG_DATABASE, MARK_NEXT_CHANGESET_RAN);
        addCategory("Inspect", SHOW_CHANGELOG_STATUS, SHOW_CHANGELOG_HISTORY);
        addCategory("Maintenance", SYNCHRONIZE_CHANGELOG, RELEASE_LOCKS, CLEAR_CHECKSUMS);
        addCategory("PreviewSql", UPDATE_SQL, ROLLBACK_SQL, SYNCHRONIZE_CHANGELOG_SQL);
        addCategory("More", DROP_ALL);
    }

    private void addCategory(@NotNull String key, @NotNull LiquibaseOperation... operations) {
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
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 8, 0));
        itemPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JPanel detailsPanel = new JPanel();
        verticalBoxLayout(detailsPanel);
        detailsPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JTextPane description = new JTextPane();
        description.setEditable(false);
        description.setOpaque(false);
        description.setFocusable(false);
        description.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        description.setText(operation.getDescription());
        description.setPreferredSize(new Dimension(150, 45));
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        description.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JLabel nameLabel = new JBLabel(operation.getName());
        nameLabel.setFont(Fonts.regular(1));
        nameLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        detailsPanel.add(nameLabel);
        detailsPanel.add(description);

        JButton openButton = new JButton(txt("app.liquibase.action.Open"));
        openButton.addActionListener(e -> executeOperation(operation));
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.add(openButton, BorderLayout.NORTH);
        itemPanel.add(detailsPanel, BorderLayout.CENTER);
        itemPanel.add(actionPanel, BorderLayout.EAST);
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, itemPanel.getPreferredSize().height));
        parent.add(itemPanel);
    }

    private void executeOperation(@NotNull LiquibaseOperation operation) {
        Project project = schema.getProject();
        DatabaseLiquibaseManager manager = DatabaseLiquibaseManager.getInstance(project);
        LiquibaseWorkspaceBundle workspaces = manager.getWorkspaces();
        LiquibaseOperationSupport support = operation.getSupport();
        if (support.requiresWorkspace() && !support.supportsWorkspaceCreation()
                && !workspaces.containsWorkspaces(schema.getConnection().getDatabaseType())) {
            showInfoDialog(
                    project,
                    txt("msg.liquibase.title.WorkspaceRequired"),
                    txt("msg.liquibase.message.NoWorkspacesAvailable", schema.getConnection().getDatabaseType().getName()),
                    new String[]{txt("msg.liquibase.button.OpenWorkspaces"), txt("msg.shared.button.Cancel")},
                    0,
                    option -> { if (option == 0) manager.openWorkspaceSettings(); });
            return;
        }

        show(() -> new LiquibaseExecutionInputDialog(schema, operation, workspaces),
                whenOk(dialog -> {
                    LiquibaseExecutionInput input = dialog.getExecutionInput();
                    manager.executeOperation(input, null);
                }));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return operationsPanel;
    }
}
