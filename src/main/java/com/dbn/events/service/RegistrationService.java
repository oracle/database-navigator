package com.dbn.events.service;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.events.listener.model.DataChangeListener;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_CALLBACK;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_CHANGELAG;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_OPERATIONS;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_REGFLAGS;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_REGID;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_TABLE_NAME;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_TIMEOUT;
import static com.dbn.events.listener.model.DataChangeListenerBundle.COL_USERNAME;

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


}
