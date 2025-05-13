package com.dbn.events;

import com.dbn.connection.ConnectionId;
import com.dbn.events.notification.model.DataChangeEvent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

public class NotificationHandler {

  public static void showIntelliJNotification(String message) {
    Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("DCN notification")
            .createNotification(message, NotificationType.INFORMATION);

    Project project = ProjectManager.getInstance().getDefaultProject();
    notification.notify(project);
  }

  public static void onChange(Project project, DataChangeEvent dataChangeEvent, @NotNull ConnectionId connectionId) {
//    DataChangeEvent dataChangeEvent = toDataChangeEvent(event);
//    Arrays.stream(event.getTableChangeDescription()).forEach(desc -> {
//      String tableName = desc.getTableName();
//      if (desc.getTableName().equalsIgnoreCase(tableName)) {
//        Arrays.stream(desc.getRowChangeDescription()).forEach(row -> {
//          String msg = "Table " + tableName + " updated. Row: " + row.getRowid();
//          System.out.println(msg);
//          showIntelliJNotification(msg);
//        });
//      }
//    });
    assert dataChangeEvent != null;
    showIntelliJNotification(dataChangeEvent.toString());
    //todo we were updating the events data , and updating the ui of the dashboard .
//    DataChangeEventBundle eventBundle = EventNotificationManager.getInstance(project).getEventBundle(connectionId);
//    eventBundle.addEvent(dataChangeEvent);
//    EventNotificationManager.getInstance(project).updateEventNotificationForm(connectionId);
  }

}


