package com.dbn.connection;

import com.dbn.assistant.interceptor.StatementExecutionInterceptor;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.cache.Cache;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.filter.Filter;
import com.dbn.common.icon.Icons;
import com.dbn.common.latent.Latent;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.common.util.TimeUtil;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionDetailSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.console.DatabaseConsoleBundle;
import com.dbn.connection.info.ConnectionInfo;
import com.dbn.connection.interceptor.DatabaseInterceptorBundle;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.session.DatabaseSessionBundle;
import com.dbn.database.DatabaseCompatibility;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseInterfaceCall;
import com.dbn.database.interfaces.DatabaseInterfaceQueue;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.queue.InterfaceQueue;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.execution.statement.StatementExecutionQueue;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.QuotePair;
import com.dbn.navigation.psi.DBConnectionPsiDirectory;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBObjectBundleImpl;
import com.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.notification.NotificationGroup.TRANSACTION;
import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.common.util.TimeUtil.isOlderThan;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.concurrent.TimeUnit.MINUTES;

@Slf4j
public class ConnectionHandlerImpl extends StatefulDisposableBase implements ConnectionHandler, NotificationSupport {

    private ConnectionSettings connectionSettings;

    private final ProjectRef project;
    private final ConnectionRef ref;

    private boolean enabled;
    private ConnectionInfo connectionInfo;
    private DatabaseCompatibility compatibility = DatabaseCompatibility.allFeatures();
    private final ConnectionInstructions instructions = new ConnectionInstructions();

    private final @Getter(lazy = true) ConnectionPool connectionPool = new ConnectionPool(this);
    private final @Getter(lazy = true) ConnectionHandlerStatusHolder connectionStatus = new ConnectionHandlerStatusHolder(this);
    private final @Getter(lazy = true) DatabaseConsoleBundle consoleBundle = new DatabaseConsoleBundle(this);
    private final @Getter(lazy = true) DatabaseSessionBundle sessionBundle = new DatabaseSessionBundle(this);
    private final @Getter(lazy = true) DatabaseInterceptorBundle interceptorBundle = new DatabaseInterceptorBundle(this);

    private final Latent<DatabaseInterfaces> interfaces = Latent.mutable(
            () -> getDatabaseType(),
            () -> DatabaseInterfacesBundle.get(this));

    private final Latent<DatabaseInterfaces> derivedInterfaces = Latent.mutable(
            () -> getDerivedDatabaseType(),
            () -> DatabaseInterfacesBundle.get(getDerivedDatabaseType()));

    private final Latent<DatabaseInterfaceQueue> interfaceQueue = Latent.basic(
            () -> new InterfaceQueue(this));

    private final Latent<DBSessionBrowserVirtualFile> sessionBrowserFile = Latent.basic(
            () -> new DBSessionBrowserVirtualFile(this));

    private final Latent<Cache> metaDataCache = Latent.basic(
            () -> new Cache(TimeUtil.Millis.ONE_MINUTE));

    private final Latent<AuthenticationInfo> temporaryAuthenticationInfo = Latent.basic(
            () -> {
                ConnectionDatabaseSettings databaseSettings = getSettings().getDatabaseSettings();
                return new AuthenticationInfo(databaseSettings, true);
            });

    private final Latent<String> debuggerVersion = Latent.basic(
            () -> {
                DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(getProject());
                return debuggerManager.getDebuggerVersion(this);
            });

    private final Latent<DBConnectionPsiDirectory> psiDirectory = Latent.basic(
            () -> new DBConnectionPsiDirectory(this));

    private final Latent<DBObjectBundle> objectBundle = Latent.basic(
            () -> new DBObjectBundleImpl(this));

    private final Map<SessionId, StatementExecutionQueue> executionQueues = new ConcurrentHashMap<>();

    ConnectionHandlerImpl(Project project, ConnectionSettings connectionSettings) {
        this.project = ProjectRef.of(project);
        this.connectionSettings = connectionSettings;
        this.enabled = connectionSettings.isActive();
        ref = ConnectionRef.of(this);

        initInterceptors();
    }

    private void initInterceptors() {
        // TODO define interceptors as application services and self inject on "connection created" event
        if (DatabaseFeature.AI_ASSISTANT.isSupported(this)) {
            getInterceptorBundle().register(StatementExecutionInterceptor.INSTANCE);
        }
    }

    @NotNull
    @Override
    public List<DBNConnection> getConnections(ConnectionType... connectionTypes) {
        return getConnectionPool().getConnections(connectionTypes);
    }

    @Override
    public ConnectionInstructions getInstructions() {
        return instructions;
    }

    @Override
    public void setTemporaryAuthenticationInfo(AuthenticationInfo temporaryAuthenticationInfo) {
        temporaryAuthenticationInfo.setTemporary(true);
        this.temporaryAuthenticationInfo.set(temporaryAuthenticationInfo);
    }

    @Override
    @Nullable
    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    @Override
    public Cache getMetaDataCache() {
        return metaDataCache.get();
    }

    @Override
    @NotNull
    public String getConnectionName(@Nullable DBNConnection connection) {
        if (connection == null) return getName();

        DatabaseSessionBundle sessionBundle = getSessionBundle();
        DatabaseSession session = sessionBundle.getSession(connection.getSessionId());
        return getName() + " (" + session.getName() + ")";
    }

    @Override
    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    @NotNull
    public AuthenticationInfo getTemporaryAuthenticationInfo() {
        AuthenticationInfo authenticationInfo = temporaryAuthenticationInfo.get();
        if (authenticationInfo.isProvided()) {
            int expiryMinutes = getSettings().getDetailSettings().getCredentialExpiryMinutes() * 60000;
            long lastAccess = getConnectionPool().getLastAccess();
            if (lastAccess > 0 &&
                    authenticationInfo.isOlderThan(expiryMinutes, MINUTES) &&
                    isOlderThan(lastAccess, expiryMinutes, MINUTES)) {
                temporaryAuthenticationInfo.reset();
            }
        }
        return temporaryAuthenticationInfo.get();
    }

    @Override
    public boolean canConnect() {
        ConnectionSettings connectionSettings = getSettings();
        if (isDisposed() || !connectionSettings.isActive()) {
            return false;
        }

        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        if (!databaseSettings.isDatabaseInitialized() && !instructions.isAllowAutoInit()) {
            return false;
        }

        ConnectionDetailSettings detailSettings = connectionSettings.getDetailSettings();
        if (detailSettings.isConnectAutomatically() || instructions.isAllowAutoConnect()) {
            return isAuthenticationProvided();
        }
        return false;
    }

    @Override
    public boolean isAuthenticationProvided() {
        return getAuthenticationInfo().isProvided() || getTemporaryAuthenticationInfo().isProvided();
    }

    @Override
    public boolean isDatabaseInitialized() {
        return getSettings().getDatabaseSettings().isDatabaseInitialized();
    }

    @Override
    @NotNull
    public ConnectionBundle getConnectionBundle() {
        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        return connectionManager.getConnectionBundle();
    }

    @NotNull
    @Override
    public ConnectionSettings getSettings() {
        return Failsafe.nn(connectionSettings);
    }

    @Override
    public void setSettings(ConnectionSettings connectionSettings) {
        this.connectionSettings = connectionSettings;
        this.enabled = connectionSettings.isActive();
    }

    @Override
    @NotNull
    public DBSessionBrowserVirtualFile getSessionBrowserFile() {
        return sessionBrowserFile.get();
    }

    @Override
    @NotNull
    public StatementExecutionQueue getExecutionQueue(SessionId sessionId) {
        return executionQueues.computeIfAbsent(sessionId, id -> new StatementExecutionQueue(this));
    }

    @Override
    @NotNull
    public PsiDirectory getPsiDirectory() {
        return psiDirectory.get();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return getSettings().getDatabaseSettings().getDatabaseType();
    }

    public DatabaseType getDerivedDatabaseType() {
        return getSettings().getDatabaseSettings().getDerivedDatabaseType();
    }

    @Override
    public double getDatabaseVersion() {
        return getSettings().getDatabaseSettings().getDatabaseVersion();
    }

    @Override
    public Filter<BrowserTreeNode> getObjectTypeFilter() {
        return getSettings().getFilterSettings().getObjectTypeFilterSettings().getElementFilter();
    }

    @NotNull
    @Override
    public EnvironmentType getEnvironmentType() {
        return getSettings().getDetailSettings().getEnvironmentType();
    }

    @Override
    public boolean isConnected() {
        return getConnectionStatus().isConnected();
    }

    @Override
    public boolean isConnected(SessionId sessionId) {
        return getConnectionPool().isConnected(sessionId);
    }

    public String toString() {
        return getName();
    }

    @Override
    @NotNull
    public Project getProject() {
        return ProjectRef.ensure(project);
    }

    @Override
    public ConnectionRef ref() {
        return ref;
    }

    @Override
    public DatabaseInfo getDatabaseInfo() {
        return getSettings().getDatabaseSettings().getDatabaseInfo();
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return getSettings().getDatabaseSettings().getAuthenticationInfo();
    }

    @Override
    public boolean isValid() {
        return getConnectionStatus().isValid();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public boolean isAutoCommit() {
        return getSettings().getPropertiesSettings().isEnableAutoCommit();
    }

    @Override
    public boolean isLoggingEnabled() {
        return getSettings().getDetailSettings().isEnableDatabaseLogging();
    }

    @Override
    public boolean hasPendingTransactions(@NotNull DBNConnection conn) {
        try {
            return ConnectionContext.surround(createConnectionContext(), () -> getMetadataInterface().hasPendingTransactions(conn));
        } catch (SQLException e) {
            conditionallyLog(e);
            sendErrorNotification(TRANSACTION, txt("ntf.transactions.error.FailedToCheckStatus", e));
            return false;

        }

    }

    @Override
    public void setLoggingEnabled(boolean loggingEnabled) {
        getSettings().getDetailSettings().setEnableDatabaseLogging(loggingEnabled);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
        getSettings().getPropertiesSettings().setEnableAutoCommit(autoCommit);
    }

    @Override
    public void disconnect() {
        // explicit disconnect (reset auto-connect data)
        temporaryAuthenticationInfo.reset();
        instructions.setAllowAutoConnect(false);
        getConnectionStatus().setConnected(false);
        getConnectionPool().closeConnections();
    }

    @Override
    @NotNull
    public ConnectionId getConnectionId() {
        return getSettings().getConnectionId();
    }

    @Override
    public String getUserName() {
        return Commons.nvl(getSettings().getDatabaseSettings().getAuthenticationInfo().getUser(), "");
    }

    @Override
    @NotNull
    public DBObjectBundle getObjectBundle() {
        return objectBundle.get();
    }

    @Override
    public SchemaId getUserSchema() {
        if (DatabaseFeature.USER_SCHEMA.isSupported(this)) {
            String userName = cachedUpperCase(getUserName());
            return SchemaId.get(userName);
        }
        return null;
    }

    @Nullable
    private SchemaId getDatabaseSchema() {
        String databaseName = getSettings().getDatabaseSettings().getDatabaseInfo().getDatabase();
        return Strings.isEmptyOrSpaces(databaseName) ? null : SchemaId.get(databaseName);
    }

    @Nullable
    private SchemaId getFirstSchema() {
        List<DBSchema> schemas = getObjectBundle().getSchemas();
        return schemas.isEmpty() ? null : SchemaId.from(schemas.get(0));
    }

    @Override
    public SchemaId getDefaultSchema() {
        return coalesce(this,
                c -> c.getUserSchema(),
                c -> c.getDatabaseSchema(),
                c -> c.getFirstSchema());
    }

    @NotNull
    @Override
    public List<SchemaId> getSchemaIds() {
        return getObjectBundle().getSchemaIds();
    }

    @Nullable
    @Override
    public SchemaId getSchemaId(String name) {
        DBSchema schema = getObjectBundle().getSchema(name);
        return schema == null ? null : schema.getIdentifier();
    }

    @Override
    public DBSchema getSchema(SchemaId schema) {
        return getObjectBundle().getSchema(schema.id());
    }

    @Override
    public DBNConnection getTestConnection() throws SQLException {
        assertCanConnect();
        return getConnectionPool().ensureTestConnection();
    }

    @Override
    @NotNull
    public DBNConnection getMainConnection() throws SQLException {
        assertCanConnect();
        return getConnectionPool().ensureMainConnection();
    }

    @Override
    @NotNull
    public DBNConnection getDebugConnection(@Nullable SchemaId schemaId) throws SQLException {
        assertCanConnect();
        DBNConnection connection = getConnectionPool().ensureDebugConnection();
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getDebuggerConnection() throws SQLException {
        assertCanConnect();
        return getConnectionPool().ensureDebuggerConnection();
    }

    @NotNull
    public DBNConnection getAssistantConnection() throws SQLException {
        assertCanConnect();
        return getConnectionPool().ensureAssistantConnection();
    }

    @Override
    @NotNull
    public DBNConnection getPoolConnection(boolean readonly) throws SQLException {
        assertCanConnect();
        return getConnectionPool().allocateConnection(readonly);
    }

    @Override
    @NotNull
    public DBNConnection getMainConnection(@Nullable SchemaId schemaId) throws SQLException {
        DBNConnection connection = getMainConnection();
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getPoolConnection(@Nullable SchemaId schemaId, boolean readonly) throws SQLException {
        DBNConnection connection = getPoolConnection(readonly);
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getConnection(@NotNull SessionId sessionId, @Nullable SchemaId schemaId) throws SQLException {
        DBNConnection connection = getConnection(sessionId);
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getConnection(@NotNull SessionId sessionId) throws SQLException {
        if (sessionId == SessionId.MAIN) return getMainConnection();
        if (sessionId == SessionId.ASSISTANT) return getAssistantConnection();
        if (sessionId == SessionId.POOL) return getPoolConnection(false);
        return getConnectionPool().ensureSessionConnection(sessionId);
    }

    @Override
    public void setCurrentSchema(DBNConnection conn, @Nullable SchemaId schema) throws SQLException {
        if (schema == null) return;
        //if (schema.isPublic()) return;
        if (!DatabaseFeature.CURRENT_SCHEMA.isSupported(this)) return;
        if (Objects.equals(schema, conn.getCurrentSchema())) return;

        ConnectionContext.surround(createConnectionContext(), () -> {
            String schemaName = schema.getName();

            DatabaseCompatibilityInterface compatibility = getCompatibilityInterface();
            QuotePair quotePair = compatibility.getDefaultIdentifierQuotes();

            getMetadataInterface().setCurrentSchema(quotePair.quote(schemaName), conn);
            conn.setCurrentSchema(schema);
        });
    }


    private void assertCanConnect() throws SQLException {
        if (!canConnect()) {
            throw Exceptions.DBN_NOT_CONNECTED_EXCEPTION;
        }
    }

    @Override
    public void closeConnection(DBNConnection connection) {
        getConnectionPool().closeConnection(connection);
    }

    @Override
    public void freePoolConnection(DBNConnection connection) {
        if (isDisposed()) return;
        ConnectionPool connectionPool = getConnectionPool();
        connectionPool.releaseConnection(connection);
    }

    @NotNull
    @Override
    public DatabaseInterfaces getInterfaces() {
        return interfaces.get();
    }

    private DatabaseInterfaces getDerivedInterfaces() {
        return derivedInterfaces.get();
    }

    public DatabaseInterfaceQueue getInterfaceQueue() {
        return interfaceQueue.get();
    }

    @Override
    public DBLanguageDialect resolveLanguageDialect(Language language) {
        if (language instanceof DBLanguageDialect) {
            return (DBLanguageDialect) language;
        } else if (language instanceof DBLanguage) {
            return getLanguageDialect((DBLanguage<?>) language);
        }
        return null;
    }

    @Override
    public DatabaseCompatibility getCompatibility() {
        return compatibility;
    }

    @Override
    public void resetCompatibilityMonitor() {
        compatibility = DatabaseCompatibility.allFeatures();
    }

    @Override
    public boolean isCloudDatabase() {
        String databaseHost = getDatabaseInfo().getHost();
        return getInterfaces().getEnvironmentInterface().isCloudDatabase(databaseHost);
    }

    @Override
    public void updateLastAccess() {
        getConnectionPool().updateLastAccess();
    }

    @Override
    public DBLanguageDialect getLanguageDialect(DBLanguage language) {
        DatabaseInterfaces interfaces = getInterfaces();
        DatabaseType databaseType = interfaces.getDatabaseType();
        if (databaseType == DatabaseType.GENERIC) {
            return getDerivedInterfaces().getLanguageDialect(language);
        }

        return interfaces.getLanguageDialect(language);
    }

    @Override
    @DatabaseInterfaceCall
    public String getDebuggerVersion() {
        return debuggerVersion.get();
    }

    /*********************************************************
     *                       TreeElement                     *
     *********************************************************/

    @Override
    @NotNull
    public String getName() {
        return getSettings().getDatabaseSettings().getName();
    }

    @Override
    public String getDescription() {
        return getSettings().getDatabaseSettings().getDescription();
    }

    @Override
    public Icon getIcon(){
        ConnectionHandlerStatusHolder connectionStatus = getConnectionStatus();
        if (connectionStatus.isConnected()) {
            return
                connectionStatus.isBusy() ? Icons.CONNECTION_BUSY :
                connectionStatus.isActive() ? Icons.CONNECTION_ACTIVE :
                    Icons.CONNECTION_CONNECTED;
        } else {
            return connectionStatus.isValid() ?
                    Icons.CONNECTION_INACTIVE :
                    Icons.CONNECTION_INVALID;
        }
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        return this;
    }

    @Override
    @Deprecated
    public boolean hasUncommittedChanges() {
        List<DBNConnection> connections = getConnections(ConnectionType.MAIN, ConnectionType.SESSION);
        for (DBNConnection connection : connections) {
            if (connection.hasDataChanges()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
