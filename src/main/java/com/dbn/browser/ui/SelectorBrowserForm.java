package com.dbn.browser.ui;

import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.CardLayouts;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Context;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusListener;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.action.AbstractConnectionAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.ui.util.UserInterface.setBackgroundRecursive;

public class SelectorBrowserForm extends DatabaseBrowserForm {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel browserFormsPanel;
    private JLabel connectionLabel;
    private JLabel connectionSelectLabel;

    private ConnectionId selectedConnectionId;
    private final Map<ConnectionId, SimpleBrowserForm> browserForms = DisposableContainers.map(this);
    private transient ListPopup popup;

    public SelectorBrowserForm(@NotNull BrowserToolWindowForm parent) {
        super(parent);

        initBrowserForms();
        connectionLabel.setCursor(Cursors.handCursor());
        connectionSelectLabel.setCursor(Cursors.handCursor());
        connectionSelectLabel.setText("");
        connectionSelectLabel.setIcon(AllIcons.General.ArrowDown);

        Mouse.Listener mouseListener = Mouse.listener().onClick(mouseEvent -> displayPopup());
        connectionLabel.addMouseListener(mouseListener);
        connectionSelectLabel.addMouseListener(mouseListener);

        Project project = ensureProject();
        ProjectEvents.subscribe(project, this, ConnectionHandlerStatusListener.TOPIC, connectionHandlerStatusListener());
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
    }

    @NotNull
    private ConnectionHandlerStatusListener connectionHandlerStatusListener() {
        return (connectionId) -> {
            if (connectionId != selectedConnectionId) return;

            ConnectionHandler connection = ConnectionHandler.get(connectionId);
            if (connection == null) return;

            //connectionLabel.setIcon(connection.getIcon());
        };
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                EnvironmentSettings environmentSettings = getEnvironmentSettings(project);
                EnvironmentVisibilitySettings visibilitySettings = environmentSettings.getVisibilitySettings();
                boolean coloredTabs = visibilitySettings.getConnectionTabs().value();

                ConnectionHandler connection = getConnection();
                EnvironmentType environmentType = connection == null || !coloredTabs ?
                        EnvironmentType.DEFAULT :
                        connection.getEnvironmentType();
                UserInterface.setBackgroundRecursive(headerPanel, environmentType.getColor());
            }
        };
    }

    public DatabaseBrowserTree getBrowserTree(ConnectionId connectionId) {
        SimpleBrowserForm browserForm = browserForms.get(connectionId);
        return browserForm == null ? null : browserForm.getBrowserTree();
    }

    @Nullable
    @Override
    public DatabaseBrowserTree getBrowserTree() {
        return getBrowserTree(selectedConnectionId);
    }

    @Override
    public void selectConnection(ConnectionId connectionId) {
        selectedConnectionId = connectionId;
        ConnectionHandler connection = getConnection();

        if (connection == null) {
            connectionLabel.setText(txt("app.connection.label.NoConnections"));
            //connectionLabel.setIcon(null);
            setBackgroundRecursive(headerPanel, EnvironmentType.DEFAULT.getColor());
            CardLayouts.showBlankCard(browserFormsPanel);
        } else {
            connectionLabel.setText(connection.getName());
            //connectionLabel.setIcon(connection.getIcon());
            setBackgroundRecursive(headerPanel, connection.getEnvironmentType().getColor());
            CardLayouts.showCard(browserFormsPanel, connectionId);
        }

        ProjectEvents.notify(ensureProject(),
                BrowserTreeEventListener.TOPIC,
                (listener) -> listener.selectionChanged());
    }

    @Nullable
    private ConnectionHandler getConnection() {
        return ConnectionHandler.get(selectedConnectionId);
    }

    private void displayPopup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        Project project = ensureProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections();
        for (ConnectionHandler connection : connections) {
            actionGroup.add(new SelectConnectionAction(connection));
        }

        popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                actionGroup,
                Context.getDataContext(this),
                false,
                false,
                false,
                () -> popup = null,
                10,
                a -> {
                    if (a instanceof SelectConnectionAction) {
                        SelectConnectionAction connectionAction = (SelectConnectionAction) a;
                        return Objects.equals(connectionAction.getConnectionId(), selectedConnectionId);
                    }
                    return false;
                });
        Popups.showUnderneathOf(popup, connectionLabel, 8, 200);
    }

    public ConnectionId getSelectedConnection() {
        return selectedConnectionId;
    }

    @Override
    public void selectElement(BrowserTreeNode treeNode, boolean focus, boolean scroll) {
        ConnectionId connectionId = treeNode.getConnectionId();
        SimpleBrowserForm browserForm = getBrowserForm(connectionId);
        if (browserForm == null) return;

        selectConnection(connectionId);
        if (scroll) browserForm.selectElement(treeNode, focus, true);
    }

    @Nullable
    private SimpleBrowserForm getBrowserForm(ConnectionId connectionId) {
        return browserForms.get(connectionId);
    }

    @Override
    public void rebuildTree() {
        browserForms.values().forEach(f -> f.rebuildTree());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void initBrowserForms() {
        JPanel mainPanel = this.mainPanel;
        if (mainPanel == null) return;

        Project project = ensureProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        for (ConnectionHandler connection : connectionBundle.getConnections()) {
            ConnectionId connectionId = connection.getConnectionId();
            if (selectedConnectionId == null) selectedConnectionId = connectionId;

            SimpleBrowserForm browserForm = new SimpleBrowserForm(this, connection);
            browserForms.put(connectionId, browserForm);
            CardLayouts.addCard(browserFormsPanel, browserForm, connectionId);
        }

        selectConnection(selectedConnectionId);
    }

    private class SelectConnectionAction extends AbstractConnectionAction {

        SelectConnectionAction(ConnectionHandler connection) {
            super(connection);
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable ConnectionHandler target) {
            ConnectionHandler connection = getConnection();
            if (connection == null) return;

            presentation.setText(Actions.adjustActionName(connection.getName()));
            presentation.setIcon(connection.getIcon());

        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connection) {
            selectConnection(connection.getConnectionId());
        }
    }
}
