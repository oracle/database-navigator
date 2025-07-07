package com.dbn.object.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.event.notification.EventNotificationManager;
import com.dbn.event.registration.EventRegistrationUtil;
import com.dbn.object.DBTable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.nls.NlsResources.txt;

public class TableEnableDCNAction extends AnObjectAction<DBTable> {
  public TableEnableDCNAction(DBTable table) {
    super(table);
  }

  @Override
  protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBTable target) {
    //todo the name should be changeable depending on if hte table already registred .
    presentation.setText(txt("app.objects.action.EnableDCN"));
    presentation.setIcon(Icons.TABLE_ENABLE_DCN);
  }

  @Override
  protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBTable target) {
    DBTable table = target;
    ConnectionHandler connection = table.getConnection();

    String processTitle = "Checking privileges";
    String processText = "Checking Privileges for " + connection.getUserName();

    Progress.prompt(project, table, false, processTitle, processText, progress -> {
      try {

        DatabaseInterfaceInvoker.execute(HIGH,
                processTitle,
                processText,
                project,
                connection.getConnectionId(),
                c -> {
                  List<String> missingPrivileges = EventRegistrationUtil.getMissingDcnPrivileges(c);
                  if (!missingPrivileges.isEmpty()) {
                    showWarningDialog(
                            project,
                            txt("msg.debugger.title.InsufficientPrivileges"),
                            txt("msg.events.error.InsufficientPrivileges", connection.getUserName(), missingPrivileges));
                  } else {
                    System.out.println("enabling DCN");
                    EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance(project);
                    eventNotificationManager.openEditorAndConfig(getTarget());

                  }

                });
      } catch (Exception ex) {
//        sendErrorNotification(project, DCN, txt("ntf.events.warning.ListenerRegistrationFailedFor", qualifiedTableName, connectionName, e.getMessage()));
      }
    });
  }

}
