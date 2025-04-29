package com.dbn.events.service;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.events.model.DataChangeRegistration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.events.model.DataChangeRegistrationBundle.*;

public class RegistrationService {

  private DataChangeRegistration mapRow(DBNResultSet rs) throws SQLException {
    return new DataChangeRegistration(
            rs.getInt(COL_REGID),
            rs.getInt(COL_REGFLAGS),
            rs.getString(COL_CALLBACK),
            rs.getInt(COL_OPERATIONS),
            rs.getInt(COL_CHANGELAG),
            rs.getLong(COL_TIMEOUT),
            rs.getString(COL_TABLE_NAME)
    );
  }

  public List<DataChangeRegistration> fetchRegistrations(ConnectionHandler connection)
          throws SQLException
  {
    return DatabaseInterfaceInvoker.load(
            HIGH,
            "Loading DCN registrations",
            "Fetching data‑change‑notification sessions…",
            connection.getProject(),
            connection.getConnectionId(),
            conn -> {
              List<DataChangeRegistration> list = new ArrayList<>();
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
