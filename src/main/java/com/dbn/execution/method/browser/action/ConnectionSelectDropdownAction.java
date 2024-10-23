package com.dbn.execution.method.browser.action;

import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.Lookups;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.database.DatabaseFeature;
import com.dbn.execution.method.browser.ui.MethodExecutionBrowserForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ConnectionSelectDropdownAction extends ComboBoxAction {
    private final MethodExecutionBrowserForm browserComponent;
    private final boolean debug;

    public ConnectionSelectDropdownAction(MethodExecutionBrowserForm browserComponent, boolean debug) {
        this.browserComponent = browserComponent;
        this.debug = debug;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        Project project = Lookups.getProject(component);
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
/*        for (ConnectionHandler virtualConnectionHandler : connectionBundle.getVirtualConnections()) {
            SelectConnectionAction connectionAction = new SelectConnectionAction(browserComponent, virtualConnectionHandler);
            actionGroup.add(connectionAction);
        }*/

        if (connectionBundle.getConnections().size() > 0) {
            //actionGroup.addSeparator();
            for (ConnectionHandler connection : connectionBundle.getConnections()) {
                if (!debug || DatabaseFeature.DEBUGGING.isSupported(connection)) {
                    ConnectionSelectAction connectionAction = new ConnectionSelectAction(browserComponent, connection);
                    actionGroup.add(connectionAction);
                }
            }
        }

        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        String text = "Select Connection";
        Icon icon = null;

        ConnectionHandler connection = browserComponent.getSettings().getConnection();
        if (connection != null) {
            text = connection.getName();
            icon = connection.getIcon();
        }

        presentation.setText(text, false);
        presentation.setIcon(icon);
    }
 }