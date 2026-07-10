package com.dbn.liquibase.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.ui.util.UserInterface.repaint;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showConfirmationDialog;
import static com.dbn.nls.NlsResources.txt;

/** Overview form containing one simple Liquibase settings card per connection. */
public class LiquibaseWorkspaceSettingsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JList<ConnectionHandler> connectionsList;

    private final LiquibaseWorkspace workspace;
    private final Map<ConnectionId, LiquibaseArtifactSettingsForm> artifactForms = DisposableContainers.map(this);
    private final Map<ConnectionId, LiquibaseArtifactPlaceholderForm> placeholderForms = DisposableContainers.map(this);
    private final Set<ConnectionId> newlyAttached = new HashSet<>();

    LiquibaseWorkspaceSettingsForm(LiquibaseWorkspaceSettingsDialog parent) {
        super(parent);
        workspace = parent.getWorkspace();
        connectionsList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value.getName(), value.getIcon(), JLabel.LEADING);
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        connectionsList.addListSelectionListener(e -> showSelectedConnection());
        updateConnections();
        if (connectionsList.getModel().getSize() > 0) connectionsList.setSelectedIndex(0);
    }

    private void updateConnections() {
        DefaultListModel<ConnectionHandler> model = new DefaultListModel<>();
        for (ConnectionHandler connection : ConnectionManager.getInstance(ensureProject()).getConnectionBundle().getConnections()) {
            model.addElement(connection);
        }
        connectionsList.setModel(model);
    }

    private void showSelectedConnection() {
        detailsPanel.removeAll();
        ConnectionHandler connection = connectionsList.getSelectedValue();
        if (connection == null) return;

        ConnectionId connectionId = connection.getConnectionId();
        DBNForm artifactForm = workspace.hasArtifact(connectionId)
                ? getArtifactForm(connection)
                : getPlaceholderForm(connection);
        detailsPanel.add(artifactForm.getComponent());
        repaint(detailsPanel);
    }

    private LiquibaseArtifactSettingsForm getArtifactForm(ConnectionHandler connection) {
        ConnectionId connectionId = connection.getConnectionId();
        return artifactForms.computeIfAbsent(connectionId, id ->
                new LiquibaseArtifactSettingsForm(this, workspace, workspace.ensureArtifact(id), connection));
    }

    private LiquibaseArtifactPlaceholderForm getPlaceholderForm(ConnectionHandler connection) {
        ConnectionId connectionId = connection.getConnectionId();
        return placeholderForms.computeIfAbsent(connectionId, id ->
                new LiquibaseArtifactPlaceholderForm(this, workspace, connection));
    }

    public void applyFormChanges() {
        artifactForms.values().forEach(LiquibaseArtifactSettingsForm::applyFormChanges);
        newlyAttached.clear();
    }

    public boolean hasChanges() {
        return !newlyAttached.isEmpty() || artifactForms.values().stream().anyMatch(LiquibaseArtifactSettingsForm::isArtifactChanged);
    }

    public void cancelFormChanges() {
        newlyAttached.forEach(workspace::removeArtifact);
    }

    void detachArtifact(ConnectionId connectionId) {
        boolean newlyAttachedArtifact = newlyAttached.remove(connectionId);
        boolean confirmed = newlyAttachedArtifact || showConfirmationDialog(ensureProject(),
                txt("msg.liquibase.title.DetachWorkspace"),
                txt("msg.liquibase.question.DetachWorkspace"),
                options(txt("msg.shared.button.Yes"), txt("msg.shared.button.No")), 1) == 0;
        if (confirmed) {
            workspace.removeArtifact(connectionId);
            artifactDetached(connectionId);
        } else {
            newlyAttached.add(connectionId);
        }
    }


    void artifactAttached(ConnectionId connectionId) {
        placeholderForms.remove(connectionId);
        newlyAttached.add(connectionId);
        getWorkspaceDialog().updateChangeState();
        showSelectedConnection();
    }

    void artifactDetached(ConnectionId connectionId) {
        artifactForms.remove(connectionId);
        placeholderForms.remove(connectionId);
        getWorkspaceDialog().updateChangeState();
        showSelectedConnection();
    }

    private LiquibaseWorkspaceSettingsDialog getWorkspaceDialog() {
        return getParentDialog();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return connectionsList;
    }
}
