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

package com.dbn.migration.shared.workflow.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskItemForm;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflowCategory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

/**
 * Project-level dashboard for grouping and starting database migration workflows.
 */
public class DatabaseMigrationWorkflowDashboardForm<
        W extends DatabaseMigrationWorkflow,
        C extends DatabaseMigrationWorkflowCategory> extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel hyperlinkPanel;
    private JTabbedPane workflowsPanel;

    private final List<DatabaseMigrationTaskItemForm> workflowForms = DisposableContainers.list(this);

    public DatabaseMigrationWorkflowDashboardForm(
            @NotNull DatabaseMigrationWorkflowDashboardDialog<W, C> parent) {
        super(parent);

        initHintPanel();
        initHyperlinkPanel();
        initWorkflowsPanel();
    }

    private void initHintPanel() {
        DatabaseMigrationWorkflowDashboardDialog<W, C> dialog = ensureParentDialog();
        hintPanel.add(new DBNHintForm(this, TextContent.plain(dialog.getDashboardHint()), null).getComponent(), BorderLayout.CENTER);
    }

    private void initHyperlinkPanel() {
        DatabaseMigrationWorkflowDashboardDialog<W, C> dialog = ensureParentDialog();
        HyperLinkForm hyperlinkForm = HyperLinkForm.create(
                "",
                dialog.getDocumentationLabel(),
                dialog.getDocumentationUrl());
        hyperlinkPanel.add(hyperlinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initWorkflowsPanel() {
        DatabaseMigrationWorkflowDashboardDialog<W, C> dialog = ensureParentDialog();
        for (C category : dialog.getCategories()) {
            addCategory(category);
        }
    }

    private void addCategory(@NotNull C category) {
        DatabaseMigrationWorkflowDashboardDialog<W, C> dialog = ensureParentDialog();
        JPanel itemsPanel = new JPanel(new BorderLayout());
        JPanel itemsListPanel = new JPanel();
        verticalBoxLayout(itemsListPanel);
        for (W workflow : dialog.getWorkflows()) {
            if (workflow.getCategory() == category) addWorkflow(itemsListPanel, workflow);
        }
        itemsPanel.add(itemsListPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        workflowsPanel.addTab(category.getName(), scrollPane);
    }

    private void addWorkflow(@NotNull JPanel parent, @NotNull W workflow) {
        DatabaseMigrationWorkflowDashboardDialog<W, C> dialog = ensureParentDialog();
        DatabaseMigrationTaskItemForm form = new DatabaseMigrationTaskItemForm(
                this,
                workflow,
                () -> {
                    dialog.doCancelAction();
                    dialog.startWorkflow(workflow);
                });
        parent.add(form.getComponent());
        workflowForms.add(form);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return workflowsPanel;
    }
}
