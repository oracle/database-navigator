/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.connection;

import com.dbn.common.notification.NotificationGroup;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.common.util.UUIDs;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNResource;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.connection.jdbc.Resource;
import com.dbn.connection.jdbc.ResourceStatus;
import com.dbn.nls.NlsResources;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.Savepoint;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.dbn.common.notification.NotificationGroup.CONNECTION;
import static com.dbn.common.notification.NotificationGroup.TRANSACTION;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.connection.jdbc.ResourceStatus.ACTIVE;
import static com.dbn.connection.jdbc.ResourceStatus.CLOSED;
import static com.dbn.connection.jdbc.ResourceStatus.VALID;
import static com.dbn.database.DatabaseFeature.READONLY_CONNECTIVITY;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.diagnostics.Diagnostics.isDatabaseResourceDebug;

@Slf4j
@UtilityClass
public final class Resources implements NlsSupport {

    public static boolean isClosed(ResultSet resultSet) throws SQLException {
        try {
            return resultSet.isClosed();
        } catch (AbstractMethodError e) {
            conditionallyLog(e);
            // sqlite AbstractMethodError for osx
            return false;
        }
    }

    public static void markClosed(DBNConnection connection) {
        if (connection == null) return;

        connection.set(VALID, false);
        connection.set(ACTIVE, false);
        connection.set(CLOSED, true);
    }

    public static void cancel(DBNStatement statement) {
        try {
            if (statement == null || statement.isClosed()) return;
            invokeResourceAction(
                    statement,
                    ResourceStatus.CANCELLING,
                    () -> statement.cancel(),
                    () -> "[DBN] Cancelling " + statement,
                    () -> "[DBN] Done cancelling " + statement,
                    () -> "[DBN] Failed to cancel " + statement);
        } catch (Throwable e) {
            conditionallyLog(e);
        } finally {
            close((DBNResource) statement);
        }
    }

    public static <T extends AutoCloseable> void close(T resource) {
        if (resource == null) return;
        if (resource instanceof DBNResource) {
            close((DBNResource) resource);
        } else {
            try {
                invokeResourceAction(
                        () -> resource.close(),
                        () -> "[DBN] Closing " + resource,
                        () -> "[DBN] Done closing " + resource,
                        () -> "[DBN] Failed to close " + resource);
            } catch (Throwable e) {
                conditionallyLog(e);
            }
        }
    }

    private static <T extends DBNResource<?>> void close(T resource) {
        if (resource == null || resource.isClosed()) return;
        try {
            resource.getListeners().notify(l -> l.closing());
            invokeResourceAction(
                    resource,
                    ResourceStatus.CLOSING,
                    () -> resource.close(),
                    () -> "[DBN] Closing " + resource,
                    () -> "[DBN] Done closing " + resource,
                    () -> "[DBN] Failed to close " + resource);
        } catch (Throwable e) {
            conditionallyLog(e);
        }
        finally {
            resource.getListeners().notify(l -> l.closed());
        }
    }

    public static <T extends AutoCloseable> void close(Collection<T> resources) {
        for (T resource : resources) {
            close(resource);
        }
    }

    public static void commitSilently(DBNConnection connection) {
        Unsafe.silent(connection, c -> commit(c));
    }

    public static void commit(DBNConnection connection) throws SQLException {
        try {
            if (connection == null || connection.isAutoCommit()) return;
            invokeResourceAction(
                    connection,
                    ResourceStatus.COMMITTING,
                    () -> connection.commit(),
                    () -> "[DBN] Committing " + connection,
                    () -> "[DBN] Done committing " + connection,
                    () -> "[DBN] Failed to commit " + connection);
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            markClosed(connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            sentWarningNotification(TRANSACTION, "ntf.connection.warning.FailedToCommit", connection, e);
            throw e;
        }
    }

    public static void rollbackSilently(DBNConnection connection) {
        Unsafe.silent(connection, c -> rollback(c));
    }

    public static void rollback(DBNConnection connection) throws SQLException {
        try {
            if (connection == null || connection.isAutoCommit()) return;
            invokeResourceAction(
                    connection,
                    ResourceStatus.ROLLING_BACK,
                    () -> connection.rollback(),
                    () -> "[DBN] Rolling-back " + connection,
                    () -> "[DBN] Done rolling-back " + connection,
                    () -> "[DBN] Failed to roll-back " + connection);
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            markClosed(connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            sentWarningNotification(TRANSACTION, "ntf.connection.warning.FailedToRollback", connection, e);
            throw e;
        }
    }

    public static void rollbackSilently(DBNConnection connection, @Nullable Savepoint savepoint) {
        Unsafe.silent(() -> rollback(connection, savepoint));
    }

    public static void rollback(DBNConnection connection, @Nullable Savepoint savepoint) throws SQLException {
        try {
            if (savepoint == null || isObsolete(connection) || connection.isAutoCommit()) return;
            String savepointId = getSavepointIdentifier(savepoint);
            invokeResourceAction(
                    connection,
                    ResourceStatus.ROLLING_BACK_SAVEPOINT,
                    () -> connection.rollback(savepoint),
                    () -> "[DBN] Rolling-back savepoint '" + savepointId + "' on " + connection,
                    () -> "[DBN] Done rolling-back savepoint '" + savepointId + "' on " + connection,
                    () -> "[DBN] Failed to roll-back savepoint '" + savepointId + "' on " + connection);
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            markClosed(connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            sentWarningNotification(TRANSACTION, "ntf.connection.warning.FailedToRollbackSavepoint", connection, e);
            throw e;
        }
    }

    @Nullable
    public static Savepoint createSavepoint(DBNConnection connection) {
        try {
            if (isObsolete(connection) || connection.isAutoCommit()) return null;
            AtomicReference<Savepoint> savepoint = new AtomicReference<>();
            invokeResourceAction(
                    connection,
                    ResourceStatus.CREATING_SAVEPOINT,
                    () -> savepoint.set(connection.setSavepoint(UUIDs.compact())),
                    () -> "[DBN] Creating savepoint on " + connection,
                    () -> "[DBN] Done creating savepoint '" + getSavepointIdentifier(savepoint.get()) + "' on " + connection,
                    () -> "[DBN] Failed to create savepoint on " + connection);
            return savepoint.get();
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            markClosed(connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            sentWarningNotification(TRANSACTION, "ntf.connection.warning.FailedToCreateSavepoint", connection, e);
        }
        return null;
    }

    public static void releaseSavepoint(DBNConnection connection, @Nullable Savepoint savepoint) {
        try {
            if (savepoint == null || isObsolete(connection) || connection.isAutoCommit()) return;
            String savepointId = getSavepointIdentifier(savepoint);
            invokeResourceAction(
                    connection,
                    ResourceStatus.RELEASING_SAVEPOINT,
                    () -> connection.releaseSavepoint(savepoint),
                    () -> "[DBN] Releasing savepoint '" + savepointId + "' on " + connection,
                    () -> "[DBN] Done releasing savepoint '" + savepointId + "' on " + connection,
                    () -> "[DBN] Failed to release savepoint '" + savepointId + "' on " + connection);
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            markClosed(connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            sentWarningNotification(TRANSACTION, "ntf.connection.warning.FailedToReleaseSavepoint", connection, e);
        }
    }

    public static void setReadonly(DBNConnection connection, boolean readonly) {
        if (READONLY_CONNECTIVITY.isNotSupported(connection.getConnectionHandler())) return;

        try {
            invokeResourceAction(
                    connection,
                    ResourceStatus.CHANGING_READ_ONLY,
                    () -> connection.setReadOnly(readonly),
                    () -> "[DBN] Applying status READ_ONLY=" + readonly + " on " + connection,
                    () -> "[DBN] Done applying status READ_ONLY=" + readonly + " on " + connection,
                    () -> "[DBN] Failed to apply status READ_ONLY=" + readonly + " on " + connection);
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            markClosed(connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            sentWarningNotification(CONNECTION, "ntf.connection.warning.FailedToChangeReadonlyStatus", connection, e);
        }
    }

    public static void setAutoCommit(DBNConnection connection, boolean autoCommit) {
        try {
            if (isObsolete(connection)) return;
            invokeResourceAction(
                    connection,
                    ResourceStatus.CHANGING_AUTO_COMMIT, () -> connection.setAutoCommit(autoCommit),
                    () -> "[DBN] Applying status AUTO_COMMIT=" + autoCommit + " on " + connection,
                    () -> "[DBN] Done applying status AUTO_COMMIT=" + autoCommit + " on " + connection,
                    () -> "[DBN] Failed to apply status AUTO_COMMIT=" + autoCommit + " on " + connection);

            connection.setAutoCommit(autoCommit);
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            markClosed(connection);
        } catch (Exception e) {
            conditionallyLog(e);
            sentWarningNotification(CONNECTION,"ntf.connection.warning.FailedToChangeAutoCommit", connection, e);
        }
    }

    private static void sentWarningNotification(NotificationGroup title, String messageKey, DBNConnection connection, Exception e) {
        String error = nvl(e.getMessage(), e.getClass().getName());
        if (connection.shouldNotify(error)) {

            Project project = connection.getProject();
            String connectionName = connection.getName();
            SessionId sessionId = connection.getSessionId();
            String errorMessage = e.getMessage();
            String message = NlsResources.txt(messageKey, connectionName, sessionId, errorMessage);

            NotificationSupport.sendWarningNotification(
                    project,
                    title,
                    message);
        }
    }

    private static <E extends Throwable> void invokeResourceAction(
            @NotNull DBNResource<?> resource,
            @NotNull ResourceStatus transientStatus,
            @NotNull ThrowableRunnable<E> action,
            @NotNull @NonNls Supplier<String> startMessage,
            @NotNull @NonNls Supplier<String> successMessage,
            @NotNull @NonNls Supplier<String> errorMessage) throws E{

        if (resource.is(transientStatus)) return;

        try {
            resource.set(transientStatus, true);
            invokeResourceAction(action, startMessage, successMessage, errorMessage);
        } finally {
            resource.set(transientStatus, false);
        }
    }

    private static <E extends Throwable> void invokeResourceAction(
            @NotNull ThrowableRunnable<E> action,
            @NotNull @NonNls Supplier<String> startMessage,
            @NotNull @NonNls Supplier<String> successMessage,
            @NotNull @NonNls Supplier<String> errorMessage) throws E{

        long start = System.currentTimeMillis();
        if (isDatabaseResourceDebug()) log.info("{}...", startMessage.get());
        try {
            action.run();
            if (isDatabaseResourceDebug()) log.info("{} - {}ms", successMessage.get(), System.currentTimeMillis() - start);
        } catch (Throwable e) {
            conditionallyLog(e);
            log.warn("{} Cause: {}", errorMessage.get(),  e.getMessage());
            throw e;
        }
    }

    public static String getSavepointIdentifier(Savepoint savepoint) {
        try {
            return savepoint.getSavepointName();
        } catch (SQLException e) {
            conditionallyLog(e);
            try {
                return Integer.toString(savepoint.getSavepointId());
            } catch (SQLException ex) {
                conditionallyLog(ex);
                return "UNKNOWN";
            }
        }
    }

    public static boolean isObsolete(@Nullable Resource resource) {
        return resource == null || resource.isObsolete();
    }
}
