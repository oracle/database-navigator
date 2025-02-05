package com.dbn.oci.actions;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.SessionId;
import com.dbn.object.DBConsole;
import com.dbn.oci.ConnectionData;
import com.dbn.oci.ui.ConnectionConfigDialog;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;

import java.awt.*;
import java.awt.event.ActionEvent;

import static com.dbn.nls.NlsResources.txt;
import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;

public class CreateConnectionDBNAction extends ContributeADBActions.ExtensionContextAction {
  ConnectionData connectionData;
  public CreateConnectionDBNAction(ConnectionData context, String title, ActionType actionType) {
    super(title,actionType);
    this.connectionData = context;
  }

  @Override
  protected void doAction(ActionEvent actionEvent) {
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project =  dataContext.getData(CommonDataKeys.PROJECT);

    Dialogs.show(() -> new ConnectionConfigDialog(project,"Connection Details", false, connectionData),(dialog,exitCode)->{
      if (exitCode == OK_EXIT_CODE) {
        // open tool window if it's not opened
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = Failsafe.nn(toolWindowManager.getToolWindow(DatabaseBrowserManager.TOOL_WINDOW_ID));
        toolWindow.show(null);

        // open db default console
        DatabaseBrowserManager databaseBrowserManager = DatabaseBrowserManager.getInstance(project);
        databaseBrowserManager.selectConnection(ConnectionId.get(connectionData.getConnectionId()));
        ConnectionHandler connectionHandler = ConnectionHandler.get(ConnectionId.get(connectionData.getConnectionId()));
        DBConsole dbConsole = connectionHandler.getConsoleBundle().getDefaultConsole();
        Editors.openFileEditor(project, dbConsole.getVirtualFile(), true);

        // connect ...
        connectionHandler.getInstructions().setAllowAutoConnect(true);
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionAction.invoke(txt("msg.connection.title.TestingConnectivity"), true, connectionHandler,
                (action) -> connectionManager.testConnection(connectionHandler, null, SessionId.MAIN, false, true));

      }
    });

  }
}
