package com.dbn.events;


import com.dbn.common.util.Titles;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.events.proxy.DcnListenerInvocationHandler;
import com.dbn.events.proxy.OracleStatementInvocationHandler;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.notification.NotificationGroup.DCN;
import static com.dbn.nls.NlsResources.txt;

public class RegistrationManager {
  // we need the key to be the table not the connection Id because one connection can have multiple registrations ...
  @Getter
  private Map<String, Object> activeRegistrations = new ConcurrentHashMap<>();
  private static RegistrationManager instance;
  Project project;
  private  ClassLoader classLoader;



  // bundle the registrations we created .
  public static synchronized RegistrationManager getInstance() {
    if (instance == null) {
      instance = new RegistrationManager();
    }
    return instance;
  }



  public void startListening(String tableName, ConnectionHandler handler, int mask) throws SQLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {

    DatabaseInterfaceInvoker.execute(HIGH,
            "Registering Event Listener",
            "Registering event listener for " + tableName,
            handler.getProject(),
            handler.getConnectionId(),
            c -> {
              try {
                registerTable(tableName, handler, c,mask);

                Notification notification = new Notification(
                        DCN.name(),            // Group ID
                        Titles.signed(DCN.name()),         // Title
                        txt("ntf.dataChangeRegistration.info.TableRegistered",tableName),                    // Message
                        NotificationType.INFORMATION // Type
                );

                Notifications.Bus.notify(notification, project);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @SneakyThrows
  public void registerTable(String tableName,
                            ConnectionHandler handler,
                            DBNConnection dbnConnection, int mask) throws Exception {
    handler.ensureConnection();
    Connection raw = DBNConnection.getInner(dbnConnection);
    this.classLoader = raw.getClass().getClassLoader();



    // --- 1) Load the public interfaces ---
    Class<?> oracleConnIfc = classLoader.loadClass("oracle.jdbc.OracleConnection");
    Class<?> dcrIfc = classLoader.loadClass("oracle.jdbc.dcn.DatabaseChangeRegistration");
    Class<?> listenerIfc = classLoader.loadClass("oracle.jdbc.dcn.DatabaseChangeListener");
    Class<?> oraStmtIfc = classLoader.loadClass("oracle.jdbc.OracleStatement");

    Properties props = buildDcnProperties(oracleConnIfc, mask);



    // --- 3) Create a registration
    Object dcr  = oracleConnIfc.getMethod(
                    "registerDatabaseChangeNotification", Properties.class)
            .invoke(raw, props);
    Long regId = (Long) dcrIfc.getMethod("getRegId").invoke(dcr);

    createDCNListner(handler, regId, dcrIfc, listenerIfc, dcr);

    tieTableToRegistration(tableName, oracleConnIfc, raw, dcr, oraStmtIfc);

    // Store for cleanup
    activeRegistrations.put(tableName, dcr);
    System.out.println("DCN registered reflectively on table: " + tableName);
  }

  private void tieTableToRegistration(String tableName, Class<?> oracleConnIfc, Connection raw, Object dcr, Class<?> oraStmtIfc) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
    Object stmtRaw = oracleConnIfc.getMethod("createStatement").invoke(raw);
    Object stmtProxy = OracleStatementInvocationHandler
            .createProxy((Statement) stmtRaw, dcr, classLoader);

    Method execQ = oraStmtIfc.getMethod("executeQuery", String.class);
    execQ.invoke(stmtProxy, "SELECT * FROM " + tableName + " WHERE 1=0");

    Method closeStmt = oraStmtIfc.getMethod("close");
    closeStmt.invoke(stmtProxy);
  }

  private void createDCNListner(ConnectionHandler handler, Long regId, Class<?> dcrIfc, Class<?> listenerIfc, Object dcr) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Object listener = DcnListenerInvocationHandler
            .createProxy(handler, classLoader, regId);
    dcrIfc.getMethod("addListener", listenerIfc)
            .invoke(dcr, listener);
  }

  private Properties buildDcnProperties(Class<?> oracleConnIfc, int mask) {
    Properties props = new Properties();

    props.setProperty(OracleConstants.DCN_NOTIFY_ROWIDS, "true");
    props.setProperty(OracleConstants.DCN_CLIENT_INIT_CONNECTION, "true");

    if ((mask & OracleConstants.DCN_NOTIFY_INSERTOP) == 0){
      props.setProperty(OracleConstants.DCN_IGNORE_INSERTOP,"true");
    }

    if ((mask & OracleConstants.DCN_NOTIFY_UPDATEOP) == 0) {
      props.setProperty(OracleConstants.DCN_IGNORE_UPDATEOP, "true");
    }

    if ((mask & OracleConstants.DCN_NOTIFY_DELETEOP) != 0) {
      props.setProperty(OracleConstants.DCN_IGNORE_DELETEOP, "true");
    }

    return props;
  }

  public void unregisterTable(String tableName, ConnectionHandler handler) throws SQLException {
    DatabaseInterfaceInvoker.execute(HIGH,
            "Stopping Event Listener",
            "Stopping event listener for " + tableName,
            handler.getProject(),
            handler.getConnectionId(),
            c -> {
              try {
                unregisterTable(tableName, c);
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
  public void unregisterTable(String tableName, DBNConnection dbnConnection) throws SQLException, ClassNotFoundException {
    //todo improve .  we make sure this method is transactional
    Object dcr = activeRegistrations.remove(tableName);

    Connection raw = DBNConnection.getInner(dbnConnection);

    Class<?> oracleConnIfc = classLoader.loadClass("oracle.jdbc.OracleConnection");

    if (dcr != null) {
      try {
        oracleConnIfc.getMethod("unregisterDatabaseChangeNotification", classLoader.loadClass("oracle.jdbc.dcn.DatabaseChangeRegistration"))
                .invoke(raw, dcr);

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





  private Long getRegId(Object dcr) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
    Class<?> dcrIfc = classLoader.loadClass("oracle.jdbc.dcn.DatabaseChangeRegistration");

    Method regIdMethod = dcrIfc.getMethod("getRegId");
    Long regId = (Long) regIdMethod.invoke(dcr);
    return regId;
  }


  public boolean isListening (String tableName) {
    return activeRegistrations.containsKey(tableName);
  }


  public boolean isActive(Long regId) {
    return activeRegistrations.values().stream()
            .anyMatch(v-> {
              try {
                return regId.equals(getRegId(v));
              } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException(e);
              }
            });
  }


}

