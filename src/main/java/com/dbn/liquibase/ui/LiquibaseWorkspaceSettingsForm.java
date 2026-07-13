package com.dbn.liquibase.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Strings;
import com.dbn.liquibase.model.LiquibaseArtifact;
import com.dbn.liquibase.model.LiquibaseWorkspace;
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

/** Overview form for managing the named Liquibase artifacts in a project workspace. */
public class LiquibaseWorkspaceSettingsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel artifactsPanel;
    private JPanel detailsPanel;
    private JList<LiquibaseArtifact> artifactsList;

    private final LiquibaseWorkspace workspace;
    private final Map<String, LiquibaseArtifactSettingsForm> artifactForms = DisposableContainers.map(this);

    LiquibaseWorkspaceSettingsForm(LiquibaseWorkspaceSettingsDialog parent) {
        super(parent);
        workspace = parent.getWorkspace();
        artifactsList.setCellRenderer((list, value, index, selected, focus) -> {
            String name = Strings.isEmpty(value.getName()) ? txt("app.shared.placeholder.Unnamed") : value.getName();
            JLabel label = new JLabel(name, Icons.DB_LIQUIBASE, JLabel.LEADING);
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        artifactsList.addListSelectionListener(e -> showSelectedArtifact());
        artifactsPanel.removeAll();
        artifactsPanel.add(initArtifactsComponent());
        updateArtifacts();
        if (artifactsList.getModel().getSize() > 0) artifactsList.setSelectedIndex(0);
    }

    private JPanel initArtifactsComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(artifactsList);
        decorator.setAddAction(button -> addArtifact());
        decorator.setRemoveAction(button -> removeArtifact());
        decorator.setMoveUpAction(button -> moveArtifact(-1));
        decorator.setMoveDownAction(button -> moveArtifact(1));
        return createToolbarDecoratorComponent(decorator, artifactsList);
    }

    private void updateArtifacts() {
        DefaultListModel<LiquibaseArtifact> model = new DefaultListModel<>();
        workspace.getArtifactList().forEach(model::addElement);
        artifactsList.setModel(model);
    }

    private void showSelectedArtifact() {
        detailsPanel.removeAll();
        LiquibaseArtifact artifact = artifactsList.getSelectedValue();
        if (artifact == null) return;

        DBNForm artifactForm = artifactForms.computeIfAbsent(artifact.getId(), id ->
                new LiquibaseArtifactSettingsForm(this, workspace, artifact));
        detailsPanel.add(artifactForm.getComponent());
        repaint(detailsPanel);
    }

    private void addArtifact() {
        LiquibaseArtifact artifact = workspace.createArtifact();
        updateArtifacts();
        artifactsList.setSelectedValue(artifact, true);
        markFormChanged();
    }

    private void removeArtifact() {
        LiquibaseArtifact artifact = artifactsList.getSelectedValue();
        if (artifact == null) return;
        workspace.removeArtifact(artifact.getId());
        artifactForms.remove(artifact.getId());
        updateArtifacts();
        if (!artifactsList.isSelectionEmpty()) showSelectedArtifact();
        markFormChanged();
    }

    private void moveArtifact(int offset) {
        LiquibaseArtifact artifact = artifactsList.getSelectedValue();
        if (artifact == null) return;
        workspace.moveArtifact(artifact, offset);
        updateArtifacts();
        artifactsList.setSelectedValue(artifact, true);
        markFormChanged();
    }

    public void applyFormChanges() {
        artifactForms.values().forEach(LiquibaseArtifactSettingsForm::applyFormChanges);
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
        return artifactsList;
    }
}
