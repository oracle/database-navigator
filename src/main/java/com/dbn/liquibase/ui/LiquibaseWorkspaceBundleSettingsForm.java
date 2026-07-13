package com.dbn.liquibase.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Strings;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.util.Map;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;
import static com.dbn.common.ui.util.UserInterface.repaint;
import static com.dbn.nls.NlsResources.txt;

/** Overview form for managing the named Liquibase workspaces in a project. */
public class LiquibaseWorkspaceBundleSettingsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel workspacesPanel;
    private JPanel detailsPanel;
    private JList<LiquibaseWorkspace> workspacesList;

    private final LiquibaseWorkspaceBundle workspaces;
    private final Map<String, LiquibaseWorkspaceSettingsForm> workspaceForms = DisposableContainers.map(this);

    LiquibaseWorkspaceBundleSettingsForm(LiquibaseWorkspaceBundleSettingsDialog parent) {
        super(parent);
        workspaces = parent.getWorkspaces();
        workspacesList.setCellRenderer((list, value, index, selected, focus) -> {
            String name = Strings.isEmpty(value.getName()) ? txt("app.shared.placeholder.Unnamed") : value.getName();
            JLabel label = new JLabel(name, Icons.DB_LIQUIBASE, JLabel.LEADING);
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        workspacesList.addListSelectionListener(e -> showSelectedWorkspace());
        workspacesPanel.removeAll();
        workspacesPanel.add(initWorkspacesList());
        updateWorkspaces();
        if (workspacesList.getModel().getSize() > 0) workspacesList.setSelectedIndex(0);
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
        DefaultListModel<LiquibaseWorkspace> model = new DefaultListModel<>();
        workspaces.getWorkspaceList().forEach(model::addElement);
        workspacesList.setModel(model);
    }

    private void showSelectedWorkspace() {
        detailsPanel.removeAll();
        LiquibaseWorkspace workspace = workspacesList.getSelectedValue();
        if (workspace == null) return;

        DBNForm workspaceForm = workspaceForms.computeIfAbsent(workspace.getId(), id ->
                new LiquibaseWorkspaceSettingsForm(this, workspaces, workspace));
        detailsPanel.add(workspaceForm.getComponent());
        repaint(detailsPanel);
    }

    void refreshWorkspaceList() {
        workspacesList.repaint();
    }

    private void addWorkspace() {
        LiquibaseWorkspace workspace = workspaces.createWorkspace();
        updateWorkspaces();
        workspacesList.setSelectedValue(workspace, true);
        markFormChanged();
    }

    private void removeWorkspace() {
        LiquibaseWorkspace workspace = workspacesList.getSelectedValue();
        if (workspace == null) return;
        workspaces.removeWorkspace(workspace.getId());
        workspaceForms.remove(workspace.getId());
        updateWorkspaces();
        if (!workspacesList.isSelectionEmpty()) showSelectedWorkspace();
        markFormChanged();
    }

    private void moveWorkspace(int offset) {
        LiquibaseWorkspace workspace = workspacesList.getSelectedValue();
        if (workspace == null) return;
        workspaces.moveWorkspace(workspace, offset);
        updateWorkspaces();
        workspacesList.setSelectedValue(workspace, true);
        markFormChanged();
    }

    public void applyFormChanges() {
        workspaceForms.values().forEach(f -> f.applyFormChanges());
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
