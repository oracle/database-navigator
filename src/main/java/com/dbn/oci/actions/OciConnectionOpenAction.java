package com.dbn.oci.actions;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.SessionId;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.object.DBConsole;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;

import java.awt.event.ActionEvent;
import java.util.Collections;

import static com.dbn.nls.NlsResources.txt;

public class OciConnectionOpenAction extends ContributeADBActions.ExtensionContextAction{
  private ConnectionSettings connectionSettings;
  public OciConnectionOpenAction(ConnectionSettings context, String title) {
    super(title, Collections.emptyList());
    this.connectionSettings = context;
  }
  @Override
  protected void doAction(ActionEvent actionEvent) {
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project =  dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    // open tool window if it's not opened
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = Failsafe.nn(toolWindowManager.getToolWindow(DatabaseBrowserManager.TOOL_WINDOW_ID));
    toolWindow.show(null);

    // open db default console
    DatabaseBrowserManager databaseBrowserManager = DatabaseBrowserManager.getInstance(project);
    databaseBrowserManager.selectConnection(connectionSettings.getConnectionId());
    ConnectionHandler connectionHandler = ConnectionHandler.get(connectionSettings.getConnectionId());
    DBConsole dbConsole = connectionHandler.getConsoleBundle().getDefaultConsole();
    Editors.openFileEditor(project, dbConsole.getVirtualFile(), true);
    connectionHandler.getInstructions().setAllowAutoConnect(true);
    ConnectionManager connectionManager = ConnectionManager.getInstance(project);

    // connect ...
    ConnectionAction.invoke(txt("msg.connection.title.TestingConnectivity"), true, connectionHandler,
            (action) -> connectionManager.testConnection(connectionHandler, null, SessionId.MAIN, false, true));
  }
}
