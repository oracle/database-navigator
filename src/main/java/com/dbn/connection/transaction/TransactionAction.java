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

package com.dbn.connection.transaction;

import com.dbn.common.constant.Constant;
import com.dbn.common.notification.NotificationGroup;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.intellij.notification.NotificationType;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum TransactionAction implements Serializable, Constant<TransactionAction> {
    COMMIT(
            txt("app.transactions.action.Commit"),
            NotificationGroup.TRANSACTION,
            NotificationType.INFORMATION, "ntf.transactions.info.Commit",
            NotificationType.ERROR, "ntf.transactions.error.Commit",
            false,
            (connection, target) -> Resources.commit(target)),

    ROLLBACK(
            txt("app.transactions.action.Rollback"),
            NotificationGroup.TRANSACTION,
            NotificationType.INFORMATION, "ntf.transactions.info.Rollback",
            NotificationType.ERROR, "ntf.transactions.error.Rollback",
            false,
            (connection, target) -> Resources.rollback(target)),

    ROLLBACK_IDLE(
            txt("app.transactions.action.IdleRollback"),
            NotificationGroup.TRANSACTION,
            NotificationType.INFORMATION, "ntf.transactions.info.Rollback",
            NotificationType.ERROR, "ntf.transactions.error.Rollback",
            false,
            (connection, target) -> Resources.rollback(target)),

    DISCONNECT(
            txt("app.transactions.action.Disconnect"),
            NotificationGroup.SESSION,
            NotificationType.INFORMATION, "ntf.transactions.info.Disconnect",
            NotificationType.WARNING, "ntf.transactions.warning.Disconnect",
            true,
            (connection, target) -> connection.closeConnection(target)),

    DISCONNECT_IDLE(
            txt("app.transactions.action.IdleDisconnect"),
            NotificationGroup.SESSION,
            NotificationType.INFORMATION, "ntf.transactions.info.DisconnectIdle",
            NotificationType.WARNING, "ntf.transactions.warning.Disconnect",
            true,
            (connection, target) -> connection.closeConnection(target)),

    KEEP_ALIVE(
            txt("app.transactions.action.KeepAlive"),
            NotificationGroup.CONNECTION,
            null, null,
            NotificationType.ERROR, "ntf.transactions.error.KeepAlive",
            false,
            (connection, target) -> target.updateLastAccess()),

    TURN_AUTO_COMMIT_ON(
            txt("app.transactions.action.EnableAutoCommit"),
            NotificationGroup.TRANSACTION,
            NotificationType.WARNING, "ntf.transactions.warning.EnableAutoCommit",
            NotificationType.ERROR, "ntf.transactions.error.EnableAutoCommit",
            true,
            (connection, target) -> target.setAutoCommit(true)),

    TURN_AUTO_COMMIT_OFF(
            txt("app.transactions.action.DisableAutoCommit"),
            NotificationGroup.TRANSACTION,
            NotificationType.INFORMATION, "ntf.transactions.info.DisableAutoCommit",
            NotificationType.ERROR, "ntf.transactions.error.DisableAutoCommit",
            true,
            (connection, target) -> target.setAutoCommit(false));


    private final NotificationGroup group;
    private final String name;
    private final String successNotificationMessage;
    private final String failureNotificationMessage;
    private final NotificationType notificationType;
    private final NotificationType failureNotificationType;
    private final Executor executor;
    private final boolean statusChange;

    TransactionAction(
            String name,
            NotificationGroup group,
            NotificationType notificationType,
            @Nls String successNotificationMessage,
            NotificationType failureNotificationType,
            @Nls String failureNotificationMessage,
            boolean statusChange,
            Executor executor) {
        this.group = group;
        this.name = name;
        this.failureNotificationMessage = failureNotificationMessage;
        this.successNotificationMessage = successNotificationMessage;
        this.executor = executor;
        this.statusChange = statusChange;
        this.notificationType = notificationType;
        this.failureNotificationType = failureNotificationType;
    }

    @FunctionalInterface
    private interface Executor {
        void execute(@NotNull ConnectionHandler connection, @NotNull DBNConnection target) throws SQLException;
    }

    public void execute(@NotNull ConnectionHandler connection, @NotNull DBNConnection target) throws SQLException {
        executor.execute(connection, target);
    }

    public static List<TransactionAction> actions(TransactionAction ... actions) {
        return Arrays.stream(actions).filter(action -> action != null).collect(Collectors.toList());
    }

}
