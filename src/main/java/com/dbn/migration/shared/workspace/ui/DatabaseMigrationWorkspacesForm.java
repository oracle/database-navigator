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

package com.dbn.migration.shared.workspace.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.message.MessageType;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.panel.DBNBanner;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Strings;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspace;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspaceBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Map;

import static com.dbn.common.ui.CardLayouts.addCard;
import static com.dbn.common.ui.CardLayouts.getCard;
import static com.dbn.common.ui.CardLayouts.removeCard;
import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;
import static com.dbn.nls.NlsResources.txt;

/**
 * Overview form for managing the named workspaces of a database migration engine.
 *
 * <p>The workspace list and lifecycle are engine-neutral. The engine supplies
 * presentation metadata and the details form used for each workspace.</p>
 */
public class DatabaseMigrationWorkspacesForm<
        W extends DatabaseMigrationWorkspace,
        B extends DatabaseMigrationWorkspaceBundle<W>> extends DBNFormBase {
    private static final String EMPTY_CARD = "DBN_EMPTY_MIGRATION_WORKSPACES";
    private JPanel mainPanel;
    private JPanel workspacesPanel;
    private JPanel detailsPanel;
    private JList<W> workspacesList;

    private final B workspaces;
    private final DatabaseMigrationEngineType engineType;
    private final DatabaseMigrationWorkspaceFormFactory<W, B> workspaceFormFactory;
    private final Map<String, DBNForm> workspaceForms = DisposableContainers.map(this);

    DatabaseMigrationWorkspacesForm(DatabaseMigrationWorkspacesDialog<W, B> parent) {
        super(parent);
        workspaces = parent.getWorkspaces();
        engineType = parent.getEngineType();
        workspaceFormFactory = parent.getWorkspaceFormFactory();
        workspacesList.setCellRenderer((list, value, index, selected, focus) -> {
            String name = Strings.isEmpty(value.getName()) ? txt("app.shared.placeholder.Unnamed") : value.getName();
            JLabel label = new JLabel(name, engineType.getIcon(), JLabel.LEADING);
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        workspacesList.addListSelectionListener(e -> showSelectedWorkspace());
        workspacesPanel.removeAll();
        workspacesPanel.add(initWorkspacesList());
        initDetailsPanel();
        updateWorkspaces();
        if (workspacesList.getModel().getSize() > 0) {
            workspacesList.setSelectedIndex(0);
        } else {
            showSelectedWorkspace();
        }
    }

    private void initDetailsPanel() {
        addCard(detailsPanel, createEmptyDetails(), EMPTY_CARD);
    }

    private JComponent createEmptyDetails() {
        DBNBanner hintBanner = new DBNBanner(engineType.getNoWorkspacesHint(), MessageType.INFO);
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setBorder(Borders.insetBorder(0, 8,8,8));
        hintPanel.add(hintBanner, BorderLayout.NORTH);
        return hintPanel;
    }

    private JPanel initWorkspacesList() {
        ToolbarDecorator decorator = createToolbarDecorator(workspacesList);
        decorator.setAddAction(button -> addWorkspace());
        decorator.setRemoveAction(button -> removeWorkspace());
        decorator.setMoveUpAction(button -> moveWorkspace(-1));
        decorator.setMoveDownAction(button -> moveWorkspace(1));
        return createToolbarDecoratorComponent(decorator, workspacesList);
    }

    private void updateWorkspaces() {
        DefaultListModel<W> model = new DefaultListModel<>();
        workspaces.getWorkspaces().forEach(model::addElement);
        workspacesList.setModel(model);
    }

    private void showSelectedWorkspace() {
        W workspace = workspacesList.getSelectedValue();
        if (workspace == null) {
            showCard(detailsPanel, EMPTY_CARD);
            return;
        }

        DBNForm workspaceForm = workspaceForms.computeIfAbsent(workspace.getId(), id ->
                workspaceFormFactory.create(this, workspaces, workspace));
        if (getCard(detailsPanel, workspace.getId()) == null) {
            addCard(detailsPanel, workspaceForm, workspace.getId());
        }
        showCard(detailsPanel, workspace.getId());
    }

    public void refreshWorkspaceList() {
        workspacesList.repaint();
    }

    private void addWorkspace() {
        W workspace = workspaces.createWorkspace();
        updateWorkspaces();
        workspacesList.setSelectedValue(workspace, true);
        markFormChanged();
    }

    private void removeWorkspace() {
        W workspace = workspacesList.getSelectedValue();
        if (workspace == null) return;
        int selectedIndex = workspacesList.getSelectedIndex();
        workspaces.removeWorkspace(workspace.getId());
        workspaceForms.remove(workspace.getId());
        removeCard(detailsPanel, workspace.getId());
        updateWorkspaces();
        int nextIndex = Math.min(selectedIndex, workspacesList.getModel().getSize() - 1);
        if (nextIndex >= 0) {
            workspacesList.setSelectedIndex(nextIndex);
        } else {
            showSelectedWorkspace();
        }
        markFormChanged();
    }

    private void moveWorkspace(int offset) {
        W workspace = workspacesList.getSelectedValue();
        if (workspace == null) return;
        workspaces.moveWorkspace(workspace, offset);
        updateWorkspaces();
        workspacesList.setSelectedValue(workspace, true);
        markFormChanged();
    }

    public void applyFormChanges() throws ConfigurationException {
        for (DBNForm workspaceForm : workspaceForms.values()) {
            workspaceForm.applyFormChanges();
        }
    }

    public void cancelFormChanges() {
        // The dialog operates on a workspace clone, so cancellation needs no rollback.
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return workspacesList;
    }
}
