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

package com.dbn.event.service;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.interfaces.DatabaseInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.event.listener.model.DataChangeListener;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_CALLBACK;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_CHANGELAG;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_OPERATIONS;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_REGFLAGS;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_REGID;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_TABLE_NAME;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_TIMEOUT;
import static com.dbn.event.listener.model.DataChangeListenerBundle.COL_USERNAME;

public class RegistrationService {

  private DataChangeListener mapRow(DBNResultSet rs) throws SQLException {
    return new DataChangeListener(
            rs.getString(COL_USERNAME),
            rs.getLong(COL_REGID),
            rs.getInt(COL_REGFLAGS),
            rs.getString(COL_CALLBACK),
            rs.getInt(COL_OPERATIONS),
            rs.getInt(COL_CHANGELAG),
            rs.getLong(COL_TIMEOUT),
            rs.getString(COL_TABLE_NAME)
    );
  }

  public List<DataChangeListener> fetchRegistrations(ConnectionHandler connection)
          throws SQLException
  {
    return DatabaseInterfaceInvoker.load(
            HIGH,
            "Loading DCN registrations",
            "Fetching data‑change‑notification sessions…",
            connection.getProject(),
            connection.getConnectionId(),
            conn -> {
              List<DataChangeListener> list = new ArrayList<>();
              try (DBNResultSet rs = (DBNResultSet)
                      connection.getMetadataInterface().loadDataEventRegistrations(conn)) {
                while (rs.next()) {
                  list.add(mapRow(rs));
                }
              }
              return list;
            }
    );
  }

    public List<String> getMissingDcnPrivileges(DBNConnection connection) throws SQLException {
        ConnectionHandler connectionHandler = connection.getConnectionHandler();
        ResultSet rs = connectionHandler.getMetadataInterface().checkUserPrivilegesOnNotification(connection);
        List<String> missingPrivileges = new ArrayList<>();

        if (rs.next()) {
            int hasExecute = rs.getInt("has_execute");
            int hasChangeNotification = rs.getInt("has_change_notification");

            if (hasExecute == 0) {
                missingPrivileges.add("EXECUTE ON DBMS_CHANGE_NOTIFICATION");
            }
            if (hasChangeNotification == 0) {
                missingPrivileges.add("CHANGE NOTIFICATION");
            }
        } else {
            // If no row returned, consider both missing (or handle as needed)
            missingPrivileges.add("EXECUTE ON DBMS_CHANGE_NOTIFICATION");
            missingPrivileges.add("CHANGE NOTIFICATION");
        }
        return missingPrivileges;
    }



}
