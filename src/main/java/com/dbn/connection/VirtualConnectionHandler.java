package com.dbn.connection;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.filter.Filter;
import com.dbn.common.icon.Icons;
import com.dbn.common.latent.Latent;
import com.dbn.common.project.ProjectRef;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.console.DatabaseConsoleBundle;
import com.dbn.connection.info.ConnectionInfo;
import com.dbn.connection.interceptor.DatabaseInterceptorBundle;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.session.DatabaseSessionBundle;
import com.dbn.database.DatabaseCompatibility;
import com.dbn.database.interfaces.DatabaseInterfaceQueue;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.execution.statement.StatementExecutionQueue;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBVirtualObjectBundle;
import com.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.exception.Exceptions.unsupported;
import static com.dbn.nls.NlsResources.txt;

@Getter
public class VirtualConnectionHandler extends StatefulDisposableBase implements ConnectionHandler {
    private final ConnectionId id;
    private final String name;
    private final DatabaseType databaseType;
    private final double databaseVersion;
    private final ProjectRef project;
    private final ConnectionHandlerStatusHolder connectionStatus;
    private final Map<String, String> properties = new HashMap<>();
    private final ConnectionRef ref;
    private final DBObjectBundle objectBundle;
    private final ConnectionInstructions instructions = new ConnectionInstructions();

    private final DatabaseCompatibility compatibility = DatabaseCompatibility.noFeatures();
    private final DatabaseSessionBundle sessionBundle = new DatabaseSessionBundle(this);

    private DatabaseInterfaces interfaces;

    private final Latent<ConnectionSettings> connectionSettings = Latent.basic(() -> {
        ConnectionBundleSettings connectionBundleSettings = ConnectionBundleSettings.getInstance(getProject());
        return new ConnectionSettings(connectionBundleSettings);
    });

    public VirtualConnectionHandler(ConnectionId id, String name, DatabaseType databaseType, double databaseVersion, @NotNull ConnectionBundle connectionBundle) {
        this.id = id;
        this.name = name;
        this.project = ProjectRef.of(connectionBundle.getProject());
        this.databaseType = databaseType;
        this.databaseVersion = databaseVersion;
        this.ref = ConnectionRef.of(this);
        this.connectionStatus = new ConnectionHandlerStatusHolder(this);
        this.objectBundle = new DBVirtualObjectBundle(this);

        Disposer.register(connectionBundle, this);
    }

    public static ConnectionHandler getDefault(Project project) {
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        return connectionManager.getConnectionBundle().getVirtualConnection(ConnectionId.VIRTUAL_ORACLE);

    }

    @Override
    public ConnectionRef ref() {
        return ref;
    }

    @Override
    public Filter<BrowserTreeNode> getObjectTypeFilter() {
        return null;
    }

    @NotNull
    @Override
    public EnvironmentType getEnvironmentType() {
        return EnvironmentType.DEFAULT;
    }

    @Override
    @Nullable
    public DBLanguageDialect resolveLanguageDialect(Language language) {
        if (language instanceof DBLanguageDialect) {
            return (DBLanguageDialect) language;
        } else if (language instanceof DBLanguage) {
            return getLanguageDialect((DBLanguage) language);
        }
        return null;
    }

    @Override
    public DBLanguageDialect getLanguageDialect(DBLanguage language) {
        return getInterfaces().getLanguageDialect(language);
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @NotNull
    public ConnectionId getConnectionId() {
        return id;
    }

    @Override
    public String getQualifiedName() {
        return "virtual " + ConnectionHandler.super.getQualifiedName();
    }

    @Override
    public String getDescription() {
        return "Virtual database connection";
    }

    @Override
    public Icon getIcon() {
        return Icons.CONNECTION_VIRTUAL;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public boolean isAutoCommit() {
        return false;
    }

    @Override
    public boolean isLoggingEnabled() {
        return false;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {}

    @Override
    public void setLoggingEnabled(boolean loggingEnabled) {}

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnected(SessionId sessionId) {
        return false;
    }

    @NotNull
    @Override
    public DatabaseInterfaces getInterfaces() {
        if (interfaces == null) {
            interfaces = DatabaseInterfacesBundle.get(this);
        }
        return interfaces;
    }

    @Override
    public DatabaseInterfaceQueue getInterfaceQueue() {
        return unsupported();
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        return this;
    }

    @Override
    public String getUserName() {
        return "root";
    }

    @Override
    public DBNConnection getTestConnection() {
        return null;
    }

    @NotNull
    @Override
    public DBNConnection getPoolConnection(boolean readonly) {
        return unsupported();
    }

    @NotNull
    @Override
    public DBNConnection getPoolConnection(@Nullable SchemaId schemaId, boolean readonly) {
        return unsupported();
    }

    @Override
    public void setCurrentSchema(DBNConnection connection, @Nullable SchemaId schema) {
    }

    @NotNull
    @Override
    public DBNConnection getMainConnection() {
        return unsupported();
    }

    @NotNull
    @Override
    public DBNConnection getDebugConnection(SchemaId schemaId) {
        return unsupported();
    }

    @NotNull
    @Override
    public DBNConnection getDebuggerConnection() {
        return unsupported();
    }

    @NotNull
    @Override
    public DBNConnection getConnection(@NotNull SessionId sessionId, @Nullable SchemaId schemaId) {
        return unsupported();
    }

    @NotNull
    @Override
    public DBNConnection getConnection(@NotNull SessionId sessionId) throws SQLException {
        return unsupported();
    }

    @NotNull
    @Override
    public DBNConnection getMainConnection(@Nullable SchemaId schemaId) {
        return unsupported();
    }

    @Override
    public void closeConnection(DBNConnection connection) {}

    @Override
    public void freePoolConnection(DBNConnection connection) {}

    @NotNull
    @Override
    public ConnectionSettings getSettings() {
        return connectionSettings.get();
    }

    @Override
    public void setSettings(ConnectionSettings connectionSettings) {
    }

    @NotNull
    @Override
    public List<DBNConnection> getConnections(ConnectionType... connectionTypes) {
        return Collections.emptyList();
    }

    @Override
    public void setTemporaryAuthenticationInfo(AuthenticationInfo temporaryAuthenticationInfo) {
    }

    @Nullable
    @Override
    public ConnectionInfo getConnectionInfo() {
        return null;
    }

    @NotNull
    @Override
    public String getConnectionName(@Nullable DBNConnection connection) {
        return getName();
    }

    @Override
    public void setConnectionInfo(ConnectionInfo connectionInfo) {
    }

    @Override
    public boolean canConnect() {
        return false;
    }

    @Override
    public boolean hasPendingTransactions(@NotNull DBNConnection conn) {
        return false;
    }

    @NotNull
    @Override
    public AuthenticationInfo getTemporaryAuthenticationInfo() {
        return unsupported();
    }

    @Override
    public boolean isAuthenticationProvided() {
        return true;
    }

    @Override
    public boolean isDatabaseInitialized() {
        return true;
    }

    @Override
    @NotNull
    public ConnectionBundle getConnectionBundle() {
        return unsupported();
    }

    @Override
    @NotNull
    public ConnectionPool getConnectionPool() {
        return unsupported();
    }

    @Override
    public SchemaId getUserSchema() {
        return null;
    }

    @Override
    public SchemaId getDefaultSchema() {
        return null;
    }

    @NotNull
    @Override
    public List<SchemaId> getSchemaIds() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public SchemaId getSchemaId(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBSchema getSchema(SchemaId schema) {
        return null;
    }

    @Override
    @NotNull
    public DBSessionBrowserVirtualFile getSessionBrowserFile() {
        return unsupported();
    }

    @NotNull
    @Override
    public PsiDirectory getPsiDirectory() {
        return unsupported();
    }

    @NotNull
    @Override
    public DatabaseConsoleBundle getConsoleBundle() {
        return unsupported();
    }

    @NotNull
    @Override
    public DatabaseSessionBundle getSessionBundle() {
        return unsupported();
    }

    @Override
    public DatabaseInterceptorBundle getInterceptorBundle() {
        return unsupported();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void disconnect() {}

    @Override
    public DatabaseInfo getDatabaseInfo() {
        return databaseType.getUrlPatterns()[0].getDefaultInfo();
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return null;
    }

    @Override
    public boolean hasUncommittedChanges() {
        return false;
    }

    @Nullable
    @Override
    public StatementExecutionQueue getExecutionQueue(SessionId sessionId) {
        return null;
    }

    @Override
    public String getDebuggerVersion() {
        return txt("app.shared.label.Unknown");
    }

    @Override
    public boolean isCloudDatabase() {
        return false;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
