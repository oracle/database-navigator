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

package com.dbn.event.registration;


import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.data.Data;
import com.dbn.common.reflection.ObjectProxies;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.event.OracleConstants;
import com.dbn.event.model.DatabaseChangeListener;
import com.dbn.event.model.DatabaseChangeRegistration;
import com.dbn.event.model.OracleConnection;
import com.dbn.event.model.OracleStatement;
import com.dbn.event.model.RowChangeDescription;
import com.dbn.event.model.TableChangeDescription;
import com.dbn.event.notification.model.DataChangeNotification;
import com.dbn.event.service.EventHistoryService;
import com.dbn.object.DBTable;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Properties;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.notification.NotificationCategory.DCN;
import static com.dbn.common.util.Lists.toCsv;
import static com.dbn.event.registration.EventRegistrationManager.COMPONENT_NAME;
import static com.dbn.nls.NlsResources.txt;


@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
public class EventRegistrationManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.EventRegistrationManager";

    private final EventRegistrationCache registrationCache = new EventRegistrationCache();

    public EventRegistrationManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static EventRegistrationManager getInstance(Project project) {
        return Components.projectService(project, EventRegistrationManager.class);
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
                ConnectionId connectionId = connection.getConnectionId();
                String tableName = table.getQualifiedName();
                DatabaseInterfaceInvoker.execute(HIGH,
                        processTitle,
                        processText,
                        project,
                        connectionId,
                        c -> registerTable(tableName, mask, connectionId, c));

                sendInfoNotification(DCN, txt("ntf.events.info.ListenerRegisteredFor", qualifiedTableName, connectionName));
            } catch (Exception e) {
                sendErrorNotification(DCN, txt("ntf.events.warning.ListenerRegistrationFailedFor", qualifiedTableName, connectionName, e.getMessage()));
            }
        });
    }

    @SneakyThrows
    public void registerTable(String tableName, int mask, ConnectionId connectionId, Connection conn) {
        Connection rawConnection = DBNConnection.getInner(conn);

        Properties properties = buildDcnProperties(mask);
        OracleConnection connection = ObjectProxies.create(rawConnection, OracleConnection.class);
        DatabaseChangeRegistration registration = connection.registerDatabaseChangeNotification(properties);
        long regId = registration.getRegId();

        DatabaseChangeListener listener = event -> {
            long eventRegId = event.getRegId();
            if (regId != eventRegId) return;
            TableChangeDescription[] tableChanges = event.getTableChangeDescription();
            for (TableChangeDescription tableChange : tableChanges) {
                String eventTableName = tableChange.getTableName();
                if (!tableName.equals(eventTableName)) continue;


                RowChangeDescription[] rowChanges = tableChange.getRowChangeDescription();
                for (RowChangeDescription rowChange : rowChanges) {
                    String rowId = rowChange.getRowid().toString();
                    String operation = toCsv(rowChange.getRowOperations(), ", ", o -> Data.asString(o));

                    String timestamp = LocalDateTime.now().toString();
                    DataChangeNotification notification = new DataChangeNotification(operation, eventTableName, rowId, timestamp, eventRegId, connectionId);
                    EventHistoryService.getInstance().pushEvent(connectionId, eventRegId, notification);
                }
            }
        };

        registration.addListener(listener);

        try (OracleStatement statement = connection.createStatement()) {
            statement.setDatabaseChangeRegistration(registration);
            statement.executeQuery("SELECT * FROM " + tableName + " WHERE 1=0");
        }
        registrationCache.addRegistration(connectionId, tableName, registration);
    }

    private Properties buildDcnProperties(int mask) {
        Properties props = new Properties();

        props.setProperty(OracleConstants.DCN_NOTIFY_ROWIDS, "true");
        props.setProperty(OracleConstants.DCN_CLIENT_INIT_CONNECTION, "true");

        if ((mask & OracleConstants.DCN_NOTIFY_INSERTOP) == 0) {
            props.setProperty(OracleConstants.DCN_IGNORE_INSERTOP, "true");
        }

        if ((mask & OracleConstants.DCN_NOTIFY_UPDATEOP) == 0) {
            props.setProperty(OracleConstants.DCN_IGNORE_UPDATEOP, "true");
        }

        if ((mask & OracleConstants.DCN_NOTIFY_DELETEOP) == 0) {  // <-- changed from != 0 to == 0
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
                String tableName = table.getQualifiedName();
                ConnectionId connectionId = connection.getConnectionId();
                DatabaseInterfaceInvoker.execute(HIGH,
                        processTitle,
                        processText,
                        project,
                        connectionId,
                        c -> unregisterTable(tableName, connectionId, c));

                sendInfoNotification(DCN, txt("ntf.events.info.ListenerDeregisteredFor", qualifiedTableName, connectionName));

            } catch (Exception e) {
                sendErrorNotification(DCN, txt("ntf.events.warning.ListenerDeregistrationFailedFor", qualifiedTableName, connectionName, e.getMessage()));
            }
        });
    }

    public void unregisterListener(Long regId, ConnectionHandler connection, String tableName, Runnable callback) {
        Project project = connection.getProject();
        String connectionName = connection.getName();

        String processTitle = "Stopping Event Listener";
        String processText = "Stopping event listener for " + tableName;

        Progress.prompt(project, connection, false, processTitle, processText, progress -> {
            try {
                ConnectionId connectionId = connection.getConnectionId();
                DatabaseInterfaceInvoker.execute(HIGH,
                        processTitle,
                        processText,
                        project,
                        connectionId,
                        c -> unregisterListener(regId, tableName, connectionId, c, callback));

                sendInfoNotification(DCN, txt("ntf.events.info.ListenerDeregisteredFor", tableName, connectionName));
            } catch (Exception e) {
                sendErrorNotification(DCN, txt("ntf.events.warning.ListenerDeregistrationFailedFor", tableName, connectionName, e.getMessage()));
            }
        });
    }

    @SneakyThrows
    private void unregisterListener(long regId, String tableName, ConnectionId connectionId, DBNConnection conn, Runnable callback) {

      // TODO fails with missing privileges
      OracleConnection connection = createProxy(conn);
      connection.unregisterDatabaseChangeNotification((int) regId);


        String creationStatement = "{ call DBMS_CHANGE_NOTIFICATION.deregister(?) }";

        PreparedStatement statement = null;

        try {
            statement = conn.prepareStatement(creationStatement);
            statement.setLong(1, regId);
            statement.execute();
        } finally {
            Resources.close(statement);
        }
        registrationCache.removeRegistration(connectionId, tableName);
        callback.run();
    }

    // Stop listening for changes on a given table
    @SneakyThrows
    private void unregisterTable(String tableName, ConnectionId connectionId, Connection conn) {
        OracleConnection connection = createProxy(conn);
        DatabaseChangeRegistration registration = registrationCache.getRegistration(connectionId, tableName);
        if (registration == null) return;

        connection.unregisterDatabaseChangeNotification(registration);
        registrationCache.removeRegistration(connectionId, tableName);
    }

    private static OracleConnection createProxy(Connection connection) {
        Connection rawConnection = DBNConnection.getInner(connection);
        return ObjectProxies.create(rawConnection, OracleConnection.class);
    }
}

