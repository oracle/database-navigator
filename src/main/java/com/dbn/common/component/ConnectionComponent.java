package com.dbn.common.component;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * Abstract stub for components revolving around a {@link ConnectionHandler}
 */
public abstract class ConnectionComponent {
    private final ConnectionRef connection;

    public ConnectionComponent(@NotNull ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public DBNConnection getConnection(SessionId sessionId) throws SQLException {
        return getConnection().getConnection(sessionId);
    }

    public ConnectionId getConnectionId() {
        return connection.getId();
    }
}
