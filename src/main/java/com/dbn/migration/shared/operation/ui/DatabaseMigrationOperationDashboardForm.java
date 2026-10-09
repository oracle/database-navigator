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

package com.dbn.migration.shared.operation.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineDescriptor;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineDescriptors;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.dbn.migration.shared.operation.DatabaseMigrationOperationCategory;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskItemForm;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskStarter;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.util.TabbedPanes.installStatePreservation;

/**
 * Project-level dashboard for grouping and starting database migration operations.
 */
public class DatabaseMigrationOperationDashboardForm<
        O extends DatabaseMigrationOperation,
        C extends DatabaseMigrationOperationCategory> extends DBNFormBase {
    private static final String DASHBOARD_STATE_CATEGORY = "MIGRATION_DASHBOARD";
    private static final String SELECTED_CATEGORY = "OPERATION_CATEGORY";

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel hyperlinkPanel;
    private JTabbedPane operationsPanel;

    private final List<DatabaseMigrationTaskItemForm> operationForms = DisposableContainers.list(this);

    public DatabaseMigrationOperationDashboardForm(
            @NotNull DatabaseMigrationOperationDashboardDialog<O, C> parent) {
        super(parent);

        installContextHeader(headerPanel);
        initHintPanel();
        initHyperlinkPanel();
        initOperationsPanel();
    }

    private void initHintPanel() {
        DatabaseMigrationOperationDashboardDialog<O, C> dialog = ensureParentDialog();
        hintPanel.add(new DBNHintForm(this, TextContent.plain(dialog.getDashboardHint()), null).getComponent(), BorderLayout.CENTER);
    }

    private void initHyperlinkPanel() {
        DatabaseMigrationOperationDashboardDialog<O, C> dialog = ensureParentDialog();
        HyperLinkForm hyperlinkForm = HyperLinkForm.create(
                "",
                dialog.getDocumentationLabel(),
                dialog.getDocumentationUrl());
        hyperlinkPanel.add(hyperlinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initOperationsPanel() {
        DatabaseMigrationOperationDashboardDialog<O, C> dialog = ensureParentDialog();
        for (C category : dialog.getCategories()) {
            addCategory(category);
        }
        installStatePreservation(
                operationsPanel,
                getDashboardState(),
                SELECTED_CATEGORY);
    }

    private void addCategory(@NotNull C category) {
        DatabaseMigrationOperationDashboardDialog<O, C> dialog = ensureParentDialog();
        JPanel itemsPanel = new JPanel(new BorderLayout());
        JPanel itemsListPanel = new JPanel();
        verticalBoxLayout(itemsListPanel);
        for (O operation : dialog.getOperations()) {
            if (operation.getCategory() == category) addOperation(itemsListPanel, operation);
        }
        itemsPanel.add(itemsListPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        operationsPanel.addTab(category.getName(), scrollPane);
    }

    private void addOperation(@NotNull JPanel parent, @NotNull O operation) {
        DatabaseMigrationOperationDashboardDialog<O, C> dialog = ensureParentDialog();
        DBSchema schema = getContextObject() instanceof DBSchema contextSchema ? contextSchema : null;
        DatabaseMigrationTaskItemForm form = new DatabaseMigrationTaskItemForm(
                this,
                operation,
                getTaskStarter(),
                schema,
                () -> dialog.doCancelAction());
        parent.add(form.getComponent());
        operationForms.add(form);
    }

    @NotNull
    private StateAttributes getDashboardState() {
        DatabaseMigrationEngineDescriptor<?, ?> engineDescriptor = getEngineDescriptor();
        return engineDescriptor.getState(
                ensureProject(),
                DASHBOARD_STATE_CATEGORY);
    }

    @NotNull
    private DatabaseMigrationTaskStarter getTaskStarter() {
        DatabaseMigrationEngineDescriptor<?, ?> engineDescriptor = getEngineDescriptor();
        return engineDescriptor.getTaskStarter(ensureProject());
    }

    private @NotNull DatabaseMigrationEngineDescriptor<?, ?> getEngineDescriptor() {
        DatabaseMigrationOperationDashboardDialog<O, C> dialog = ensureParentDialog();
        return DatabaseMigrationEngineDescriptors.get(dialog.getEngineType());
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
