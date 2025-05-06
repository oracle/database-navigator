package com.dbn.events.proxy;

import com.dbn.connection.ConnectionHandler;
import com.dbn.events.NotificationHandler;
import com.dbn.events.model.DataChangeEvent;
import com.dbn.events.service.EventHistoryService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class DcnListenerInvocationHandler implements InvocationHandler {
  private final ConnectionHandler handler;
  private final Long regId;

  private DcnListenerInvocationHandler(ConnectionHandler handler, Long regId) {
    this.handler = handler;
    this.regId = regId;
  }

  /**
   * Creates a proxy instance implementing the driver’s
   * oracle.jdbc.dcn.DatabaseChangeListener interface.
   */
  public static Object createProxy(ConnectionHandler handler, ClassLoader driverClassLoader,Long regId) throws ClassNotFoundException {
    Class<?> listenerIfc = driverClassLoader.loadClass("oracle.jdbc.dcn.DatabaseChangeListener");
    return java.lang.reflect.Proxy.newProxyInstance(
            driverClassLoader,
            new Class<?>[]{listenerIfc},
            new DcnListenerInvocationHandler(handler,regId)
    );
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String name = method.getName();

    if ("onDatabaseChangeNotification".equals(name) && args != null && args.length == 1) {
      // args[0] is the driver's DatabaseChangeEvent

      Object rawEvent = args[0]; // the raw event
      DataChangeEvent mappedEvent = toDataChangeEvent(rawEvent);  // Map to your DataChangeEvent class

      // Dispatch the mapped event via your NotificationHandler
      NotificationHandler.onChange(
              handler.getProject(),
              mappedEvent,  // Pass your custom DataChangeEvent
              handler.getConnectionId()
      );

      return null; // No other return value expected
    }

    // No other methods expected
    return null;
  }

  // Mapping method to convert DatabaseChangeEvent to DataChangeEvent
  private DataChangeEvent toDataChangeEvent(Object rawEvent) {
    AtomicReference<DataChangeEvent> dataChangeEvent = new AtomicReference<>();

    try {
      // Use reflection to get the TableChangeDescription array from the event getTableChangeDescription
      Method getTableChangeDescription = rawEvent.getClass().getMethod("getTableChangeDescription");
      getTableChangeDescription.setAccessible(true); // Allow access to private/package-private methods
      Object[] tableChanges = (Object[]) getTableChangeDescription.invoke(rawEvent);

      // Iterate through table descriptions
      Arrays.stream(tableChanges).forEach(desc -> {
        try {
          // Use reflection to get the table name
          Method getTableName = desc.getClass().getMethod("getTableName");
          getTableName.setAccessible(true);
          String tableName = (String) getTableName.invoke(desc);

          // Check if the table name matches your condition
          if (tableName.equalsIgnoreCase(tableName)) {
            // Reflectively get the row change description array
            Method getRowChangeDescription = desc.getClass().getMethod("getRowChangeDescription");
            getRowChangeDescription.setAccessible(true);
            Object[] rowChanges = (Object[]) getRowChangeDescription.invoke(desc);

            // Iterate through row changes
            Arrays.stream(rowChanges).forEach(row -> {

              try {
                Method getRowId = row.getClass().getMethod("getRowid");
                getRowId.setAccessible(true); // Allow access to private/package-private methods
                String rowId = String.valueOf(getRowId.invoke(row));

                Method getRowOperations = row.getClass().getMethod("getRowOperations");
                getRowOperations.setAccessible(true); // Allow access to private/package-private methods
                String operation = String.valueOf(getRowOperations.invoke(row));

                // Use current timestamp (you can use the actual timestamp from the event, if available)
                String timestamp = LocalDateTime.now().toString();
                // Create and set the mapped DataChangeEvent
                dataChangeEvent.set(new DataChangeEvent(operation, tableName, rowId, timestamp,regId, handler.getConnectionId().id()));
                EventHistoryService.getInstance().pushEvent(handler.getConnectionId().id(),this.regId,dataChangeEvent.get());
              } catch (Exception e) {
                e.printStackTrace(); // Handle potential reflection errors in row processing
              }
            });
          }
        } catch (Exception e) {
          e.printStackTrace(); // Handle potential reflection errors in table processing
        }
      });
    } catch (Exception e) {
      e.printStackTrace(); // Handle general reflection errors
    }

    return dataChangeEvent.get();
  }

}
