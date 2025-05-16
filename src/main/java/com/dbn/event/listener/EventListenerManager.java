/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.event.listener;


import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.event.OracleConstants;
import com.dbn.event.proxy.DcnListenerInvocationHandler;
import com.dbn.event.proxy.OracleStatementInvocationHandler;
import com.dbn.object.DBTable;
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
import static com.dbn.common.notification.NotificationCategory.DCN;
import static com.dbn.common.notification.NotificationSupport.sendErrorNotification;
import static com.dbn.common.notification.NotificationSupport.sendInfoNotification;
import static com.dbn.nls.NlsResources.txt;

public class EventListenerManager {
  // we need the key to be the table not the connection Id because one connection can have multiple registrations ...
  @Getter
  private Map<String, Object> activeRegistrations = new ConcurrentHashMap<>();
  private static EventListenerManager instance;
  Project project;
  private  ClassLoader classLoader;



  // bundle the registrations we created .
  public static synchronized EventListenerManager getInstance() {
    if (instance == null) {
      instance = new EventListenerManager();
    }
    return instance;
  }



  public void startListening(DBTable table, int mask) {
    ConnectionHandler connection = table.getConnection();
    Project project = connection.getProject();
    String connectionName = connection.getName();
    String qualifiedTableName = table.getQualifiedNameWithType();

    String processTitle = "Registering Event Listener";
    String processText = "Registering event listener for " + qualifiedTableName;

    Progress.prompt(project, table, false, processTitle, processText, progress -> {
      try {
        String tableName = table.getQualifiedName(true);
        DatabaseInterfaceInvoker.execute(HIGH,
                processTitle,
                processText,
                project,
                connection.getConnectionId(),
                c -> registerTable(tableName, connection, c, mask));

        sendInfoNotification(project, DCN, txt("ntf.events.info.ListenerRegisteredFor", qualifiedTableName, connectionName));

      } catch (Exception e) {
        sendErrorNotification(project, DCN, txt("ntf.events.warning.ListenerRegistrationFailedFor", qualifiedTableName, connectionName, e.getMessage()));
      }
    });
  }

  @SneakyThrows
  public void registerTable(String tableName,
                            ConnectionHandler handler,
                            DBNConnection dbnConnection, int mask) {
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
    Method registrationMethod = oracleConnIfc.getMethod(
            "registerDatabaseChangeNotification", Properties.class);
    Object dcr  = registrationMethod.invoke(raw, props);
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

  public void unregisterTable(DBTable table) {
    ConnectionHandler connection = table.getConnection();
    Project project = connection.getProject();
    String connectionName = connection.getName();
    String qualifiedTableName = table.getQualifiedNameWithType();

    String processTitle = "Stopping Event Listener";
    String processText = "Stopping event listener for " + qualifiedTableName;

    Progress.prompt(project, table, false, processTitle, processText, progress -> {
      try {
        String tableName = table.getQualifiedName(true);
        DatabaseInterfaceInvoker.execute(HIGH,
                processTitle,
                processText,
                project,
                connection.getConnectionId(),
                c -> unregisterTable(tableName, c));

        sendInfoNotification(project, DCN, txt("ntf.events.info.ListenerDeregisteredFor", qualifiedTableName, connectionName));

      } catch (Exception e) {
        sendErrorNotification(project, DCN, txt("ntf.events.warning.ListenerDeregistrationFailedFor", qualifiedTableName, connectionName, e.getMessage()));
      }
    });
  }

  // Stop listening for changes on a given table
  @SneakyThrows
  private void unregisterTable(String tableName, DBNConnection dbnConnection) {
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

