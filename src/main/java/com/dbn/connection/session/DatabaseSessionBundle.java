package com.dbn.connection.session;

import com.dbn.common.dispose.Disposed;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.index.IdentifiableMap;
import com.dbn.common.util.CollectionUtil;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.SessionId;
import com.dbn.database.DatabaseFeature;
import com.intellij.openapi.Disposable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.dbn.common.dispose.Disposer.replace;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.filtered;
import static com.dbn.common.util.Lists.first;

@Getter
public class DatabaseSessionBundle extends StatefulDisposableBase implements Disposable{
    private final
    ConnectionRef connection;
    private final DatabaseSession mainSession;
    private DatabaseSession debugSession;
    private DatabaseSession debuggerSession;
    private DatabaseSession assistantSession;
    private DatabaseSession poolSession;

    private List<DatabaseSession> sessions = CollectionUtil.createConcurrentList();
    private final IdentifiableMap<SessionId, DatabaseSession> index = new IdentifiableMap<>();

    public DatabaseSessionBundle(ConnectionHandler connection) {
        super(connection);
        this.connection = connection.ref();

        mainSession = new DatabaseSession(SessionId.MAIN, "Main", ConnectionType.MAIN, connection);
        sessions.add(mainSession);

        if (connection.isVirtual()) return;

        if (DatabaseFeature.DEBUGGING.isSupported(connection)) {
            debugSession = new DatabaseSession(SessionId.DEBUG, "Debug", ConnectionType.DEBUG, connection);
            debuggerSession = new DatabaseSession(SessionId.DEBUGGER, "Debugger", ConnectionType.DEBUGGER, connection);
            sessions.add(debugSession);
            sessions.add(debuggerSession);
        }


        if (DatabaseFeature.AI_ASSISTANT.isSupported(connection)){
            assistantSession = new DatabaseSession(SessionId.ASSISTANT, "Assistant", ConnectionType.ASSISTANT, connection);
            sessions.add(assistantSession);
        }

        poolSession = new DatabaseSession(SessionId.POOL, "Pool", ConnectionType.POOL, connection);
        sessions.add(poolSession);

        rebuildIndex();
    }

    private void rebuildIndex() {
        Collections.sort(sessions);
        this.index.rebuild(sessions);
    }

    public List<DatabaseSession> getSessions(ConnectionType ... connectionTypes) {
        List<DatabaseSession> sessions = filtered(this.sessions, session -> session.getConnectionType().matches(connectionTypes));
        sessions.sort(Comparator.comparingInt(session -> session.getConnectionType().getPriority()));
        return sessions;
    }

    public String getSessionName(SessionId sessionId) {
        DatabaseSession session = getSession(sessionId);
        return session.getName();
    }

    public Set<String> getSessionNames() {
        return sessions.stream().map(s -> s.getName()).collect(Collectors.toSet());
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public DatabaseSession getMainSession() {
        return nn(mainSession);
    }

    @NotNull
    public DatabaseSession getAssistantSession(){ return nn(assistantSession);};

    @Nullable
    public DatabaseSession getSession(String name) {
        return first(sessions, session -> Objects.equals(session.getName(), name));
    }

    public boolean hasSession(SessionId id) {
        return index.contains(id);
    }

    @NotNull
    public DatabaseSession getSession(SessionId id) {
        DatabaseSession session = index.get(id);
        return nvl(session, getMainSession());
    }

    void addSession(SessionId id, String name) {
        sessions.add(new DatabaseSession(id, name, ConnectionType.SESSION, getConnection()));
        rebuildIndex();
    }

    DatabaseSession createSession(String name) {
        ConnectionHandler connection = getConnection();
        DatabaseSession session = new DatabaseSession(null, name, ConnectionType.SESSION, connection);
        sessions.add(session);
        rebuildIndex();
        return session;
    }

    void deleteSession(SessionId id) {
        DatabaseSession session = getSession(id);
        if (session.getConnectionType() != ConnectionType.SESSION) return;

        sessions.remove(session);
        rebuildIndex();
    }

    void renameSession(String oldName, String newName) {
        DatabaseSession session = getSession(oldName);
        if (session == null) return;

        session.setName(newName);
    }

    @Override
    public void disposeInner() {
        sessions = replace(sessions, Disposed.list());
        index.clear();
        nullify();
    }
}
