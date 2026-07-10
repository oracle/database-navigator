package com.dbn.liquibase.ui;

import com.dbn.common.dispose.DisposableContainers;
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
import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.ui.util.UserInterface.repaint;

/** Overview form containing one simple Liquibase settings card per connection. */
public class LiquibaseWorkspaceSettingsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JList<ConnectionHandler> connectionsList;

    private final LiquibaseWorkspace workspace;
    private final Map<ConnectionId, LiquibaseArtifactSettingsForm> artifactForms = DisposableContainers.map(this);
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
        if (connection != null) {
            ConnectionId connectionId = connection.getConnectionId();
            if (!workspace.hasArtifact(connectionId)) {
                LiquibaseArtifactPlaceholderForm placeholder = new LiquibaseArtifactPlaceholderForm(this, connection, () -> {
                    workspace.ensureArtifact(connectionId);
                    newlyAttached.add(connectionId);
                    showSelectedConnection();
                });
                detailsPanel.add(placeholder.getComponent(), BorderLayout.CENTER);
            } else {
                LiquibaseArtifactSettingsForm form = artifactForms.get(connectionId);
                if (form == null) {
                    form = new LiquibaseArtifactSettingsForm(this, workspace, workspace.ensureArtifact(connectionId), connection);
                    artifactForms.put(connectionId, form);
                }
                detailsPanel.add(form.getComponent(), BorderLayout.CENTER);
            }
        }
        repaint(detailsPanel);
    }

    public void applyFormChanges() {
        artifactForms.values().forEach(LiquibaseArtifactSettingsForm::applyFormChanges);
    }

    public void cancelFormChanges() {
        newlyAttached.forEach(workspace::removeArtifact);
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
