package com.dbn.events;

import com.dbn.connection.ConnectionId;
import com.dbn.events.model.DataChangeEvent;
import com.dbn.events.model.DataChangeEventBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import oracle.jdbc.dcn.DatabaseChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

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
    DataChangeEventBundle eventBundle = EventNotificationManager.getInstance(project).getEventBundle(connectionId);
    eventBundle.addEvent(dataChangeEvent);
    EventNotificationManager.getInstance(project).updateEventNotificationForm(connectionId);
  }

  private static DataChangeEvent toDataChangeEvent(DatabaseChangeEvent event) {
    AtomicReference<DataChangeEvent> dataChangeEvent = new AtomicReference<>();
    Arrays.stream(event.getTableChangeDescription()).forEach(desc -> {
      String tableName = desc.getTableName();
      if (desc.getTableName().equalsIgnoreCase(tableName)) {
        Arrays.stream(desc.getRowChangeDescription()).forEach(row -> {
          String rowId = row.getRowid().toString();
          String operation = row.getRowOperations().toString();
          String timestamp = LocalDateTime.now().toString();
          dataChangeEvent.set(new DataChangeEvent(operation, tableName, rowId, timestamp));
        });
      }
    });
    return dataChangeEvent.get();
  }

}


