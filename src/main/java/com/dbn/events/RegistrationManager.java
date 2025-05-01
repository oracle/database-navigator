package com.dbn.events;


import com.dbn.common.util.Titles;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.events.model.DataChangeEvent;
import com.dbn.events.service.HistoryService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import oracle.jdbc.driver.OracleConnection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.notification.NotificationGroup.DCN;
import static com.dbn.nls.NlsResources.txt;

public class RegistrationManager {
  // we need the key to be the table not the connection Id because one connection can have multiple registrations ...
  private Map<String, Object> activeRegistrations = new ConcurrentHashMap<>();
  private static RegistrationManager instance;
  Project project;

  // bundle the registrations we created .
  public static synchronized RegistrationManager getInstance() {
    if (instance == null) {
      instance = new RegistrationManager();
    }
    return instance;
  }



  public void startListening(String tableName, ConnectionHandler handler,int mask) throws SQLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {

    DatabaseInterfaceInvoker.execute(HIGH,
            "Registering Event Listener",
            "Registering event listener for " + tableName,
            handler.getProject(),
            handler.getConnectionId(),
            c -> {
              try {
                startListening(tableName, handler, c,mask);

                Notification notification = new Notification(
                        DCN.name(),            // Group ID
                        Titles.signed(DCN.name()),         // Title
                        txt("ntf.dataChangeRegistration.info.TableRegistered",tableName),                    // Message
                        NotificationType.INFORMATION // Type
                );

                Notifications.Bus.notify(notification, project);
//                NotificationSupport.sendInfoNotification(project,DCN, txt("ntf.dataChangeRegistration.info.TableRegistered",tableName));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @SneakyThrows
  public void startListening(String tableName,
                             ConnectionHandler handler,
                             DBNConnection dbnConnection, int mask) throws Exception {
    handler.ensureConnection();
    Connection raw = DBNConnection.getInner(dbnConnection);
    ClassLoader drvCL = raw.getClass().getClassLoader();



    // --- 1) Load the public interfaces ---
    Class<?> oracleConnIfc = drvCL.loadClass("oracle.jdbc.OracleConnection");
    Class<?> dcrIfc = drvCL.loadClass("oracle.jdbc.dcn.DatabaseChangeRegistration");
    Class<?> listenerIfc = drvCL.loadClass("oracle.jdbc.dcn.DatabaseChangeListener");
    Class<?> oraStmtIfc = drvCL.loadClass("oracle.jdbc.OracleStatement");

    // --- 2) Build DCN properties via reflection of constants ---
    Properties props = new Properties();

    String notifyRowIdsKey = (String) oracleConnIfc.getField("DCN_NOTIFY_ROWIDS").get(null);
    String clientInitKey = (String) oracleConnIfc.getField("DCN_CLIENT_INIT_CONNECTION").get(null);

    props.setProperty(notifyRowIdsKey, "true");
    props.setProperty(clientInitKey, "true");

    if ((mask & OracleConstants.DCN_NOTIFY_INSERTOP) == 0){
      props.setProperty(OracleConnection.DCN_IGNORE_INSERTOP,"true");
    }

    if ((mask & OracleConstants.DCN_NOTIFY_UPDATEOP) == 0) {
      props.setProperty(OracleConstants.DCN_IGNORE_UPDATEOP, "true");
    }

    if ((mask & OracleConstants.DCN_NOTIFY_DELETEOP) != 0) {
      props.setProperty(OracleConstants.DCN_IGNORE_DELETEOP, "true");
    }
    // --- 3) registerDatabaseChangeNotification on the interface ---
    Method regM = oracleConnIfc.getMethod(
            "registerDatabaseChangeNotification", Properties.class);
    Object dcr = regM.invoke(raw, props);
    Method regIdMethod = oracleConnIfc.getMethod("getRegId");
    Long regId = (Long) regIdMethod.invoke(dcr);

    // --- 4) addListener via the public interface ---
    Object listener = DcnListenerInvocationHandler
            .createProxy(handler, drvCL,regId);
    Method addL = dcrIfc.getMethod("addListener", listenerIfc);
    addL.invoke(dcr, listener);

    // --- 5) Tie to table via OracleStatement interface ---
    Method createStmt = oracleConnIfc.getMethod("createStatement");
    Object stmtRaw = createStmt.invoke(raw);
    Object stmtProxy = OracleStatementInvocationHandler
            .createProxy((Statement) stmtRaw, dcr, drvCL);

    Method execQ = oraStmtIfc.getMethod("executeQuery", String.class);
    execQ.invoke(stmtProxy, "SELECT * FROM " + tableName + " WHERE 1=0");

    Method closeStmt = oraStmtIfc.getMethod("close");
    closeStmt.invoke(stmtProxy);

    // Store for cleanup
    activeRegistrations.put(tableName, dcr);
    System.out.println("DCN registered reflectively on table: " + tableName);
  }

  public void stopListening(String tableName, ConnectionHandler handler) throws SQLException {
    DatabaseInterfaceInvoker.execute(HIGH,
            "Stopping Event Listener",
            "Stopping event listener for " + tableName,
            handler.getProject(),
            handler.getConnectionId(),
            c -> {
              try {
                stopListening(tableName, c);
                Notification notification = new Notification(
                        DCN.name(),            // Group ID
                        Titles.signed(DCN.name()),         // Title
                        txt("ntf.dataChangeRegistration.info.TableUnregistered",tableName),                    // Message
                        NotificationType.INFORMATION // Type
                );

                Notifications.Bus.notify(notification, project);
//                NotificationSupport.sendInfoNotification(project,DCN, txt("ntf.dataChangeRegistration.info.TableUnregistered", tableName));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  // Stop listening for changes on a given table
  public void stopListening(String tableName,DBNConnection dbnConnection) throws SQLException, ClassNotFoundException {
    //todo improve .
    // Retrieve the registration object (dcr) for the given table
    Object dcr = activeRegistrations.remove(tableName);

    Connection raw = DBNConnection.getInner(dbnConnection);
    ClassLoader drvCL = raw.getClass().getClassLoader();

    // Get the DBNConnection from the connection handler

    // Use reflection to unwrap the connection to the actual OracleConnection class dynamically
    Class<?> oracleConnIfc = drvCL.loadClass("oracle.jdbc.OracleConnection");

    if (dcr != null) {
      try {
        // Use reflection to get the unregisterDatabaseChangeNotification method
        Method unregisterMethod = oracleConnIfc.getMethod("unregisterDatabaseChangeNotification", drvCL.loadClass("oracle.jdbc.dcn.DatabaseChangeRegistration"));

        // Invoke the method to unregister the change notification listener
        unregisterMethod.invoke(raw, dcr);

        System.out.println("Stopped listening on table: " + tableName);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
        // Handle exceptions related to reflection
        e.printStackTrace();
        throw new SQLException("Error while stopping DCN listener on table: " + tableName, e);
      }
    } else {
      System.out.println("No registration found for table: " + tableName);
    }
  }





  static class DcnListenerInvocationHandler implements InvocationHandler {
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
                  dataChangeEvent.set(new DataChangeEvent(operation, tableName, rowId, timestamp));
                  HistoryService.getInstance().pushEvent(this.regId,dataChangeEvent.get());
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



    static class OracleStatementInvocationHandler implements InvocationHandler {
    private final Statement delegate;
    private final Object dcr;

    private OracleStatementInvocationHandler(Statement delegate, Object dcr) {
      this.delegate = delegate;
      this.dcr = dcr;
    }

    /**
     * Creates a proxy for the driver’s OracleStatement interface.
     */
    public static Statement createProxy(Statement stmt, Object dcr, ClassLoader driverClassLoader) throws ClassNotFoundException {
      Class<?> oraStmtIfc = driverClassLoader.loadClass("oracle.jdbc.OracleStatement");
      return (Statement) java.lang.reflect.Proxy.newProxyInstance(
              driverClassLoader,
              new Class<?>[]{oraStmtIfc},
              new OracleStatementInvocationHandler(stmt, dcr)
      );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      if (name.startsWith("execute")) {
        // --- Begin change ---
        // Fetch the public OracleStatement interface (not the impl class)
        ClassLoader driverCL = delegate.getClass().getClassLoader();
        Class<?> oraStmtIfc = driverCL.loadClass("oracle.jdbc.OracleStatement");
        Class<?> dcrIfc = driverCL.loadClass("oracle.jdbc.dcn.DatabaseChangeRegistration");

        // Look up the interface method
        Method setReg = oraStmtIfc.getMethod("setDatabaseChangeRegistration", dcrIfc);
        setReg.invoke(delegate, dcr);
        // --- End change ---
      }
      // Forward all other calls
      return method.invoke(delegate, args);
    }
  }

  public boolean isListening (String tableName) {
    return activeRegistrations.containsKey(tableName);
  }
}

