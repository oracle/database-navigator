package com.dbn.connection.context;

import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.*;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.dbn.common.util.Commons.nvl;

public interface DatabaseContextBase extends DatabaseContext {

    @Nullable
    default ConnectionId getConnectionId() {
        ConnectionHandler connection = getConnection();
        return connection == null ? null : connection.getConnectionId();
    }

    @Nullable
    default SessionId getSessionId() {
        return null;
    }

    @Nullable
    default DatabaseSession getSession() {
        return null;
    }

    @Nullable
    default SchemaId getSchemaId() {
        return null;
    }

    @Override
    default boolean isSameAs(DatabaseContext that) {
        return
            Objects.equals(nvl(this.getConnectionId(), ConnectionId.NULL), nvl(that.getConnectionId(), ConnectionId.NULL)) &&
            Objects.equals(nvl(this.getSessionId(), SessionId.NULL), nvl(that.getSessionId(), SessionId.NULL)) &&
            Objects.equals(nvl(this.getSchemaId(), SchemaId.NULL), nvl(that.getSchemaId(), SchemaId.NULL));
    }

    @Nullable
    default DBSchema getSchema() {
        SchemaId schemaId = getSchemaId();
        if (schemaId == null) return null;

        ConnectionHandler connection = getConnection();
        if (connection == null) return null;

        return connection.getSchema(schemaId);
    }

    @Nullable
    @Override
    default String getSchemaName() {
        SchemaId schemaId = getSchemaId();
        return schemaId == null ? null : schemaId.getName();
    }

    @Nullable
    ConnectionHandler getConnection();

    @NotNull
    default ConnectionHandler ensureConnection() {
        return Failsafe.nn(getConnection());
    }

    @NotNull
    default DBObjectBundle getObjectBundle() {
        return ensureConnection().getObjectBundle();
    }

    @NotNull
    default DatabaseInterfaces getInterfaces() {
        return ensureConnection().getInterfaces();
    }

    default ConnectionContext createConnectionContext() {
        return new ConnectionContext(getProject(), getConnectionId(), null);
    }


}
