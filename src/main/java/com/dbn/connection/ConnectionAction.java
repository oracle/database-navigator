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

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.connection.context.DatabaseContextBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Messages.options;
import static com.dbn.nls.NlsResources.txt;

public abstract class ConnectionAction implements DatabaseContextBase {
    static final String[] OPTIONS_CONNECT_CANCEL = options(
            txt("msg.shared.button.Connect"),
            txt("msg.shared.button.Cancel"));

    private final String title;
    private final boolean interactive;
    private final DatabaseContext context;
    private boolean cancelled;

    private ConnectionAction(@DialogTitle String title, boolean interactive, DatabaseContext context) {
        this.title = title;
        this.interactive = interactive;
        this.context = context;
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    public boolean isCancelled() {
        if (cancelled) {
            return true;
        } else {
            return ProgressMonitor.isProgressCancelled();
        }
    }

    protected abstract void execute();

    protected void cancel() {
        cancelled = true;
    }

    public final void start() {
        ConnectionHandler connection = getConnection();
        if (connection.isVirtual() || connection.canConnect()) {
            if (interactive || connection.isValid()) {
                guarded(this, a -> a.execute());
            } else {
                String connectionName = connection.getName();
                Throwable connectionException = connection.getConnectionStatus().getConnectionException();
                ConnectionManager connectionManager = getConnectionManager(connection);
                connectionManager.showErrorConnectionMessage(getProject(), connectionName, connectionException);
            }
        } else {
            if (connection.isDatabaseInitialized()) {
                if (connection.isAuthenticationProvided()) {
                    promptConnectDialog();
                } else {
                    promptAuthenticationDialog();
                }
            } else {
                promptDatabaseInitDialog();
            }
        }
    }

    private void promptDatabaseInitDialog() {
        ConnectionHandler connection = getConnection();
        ConnectionManager connectionManager = getConnectionManager(connection);
        connectionManager.promptDatabaseInitDialog(
                connection,
                option -> {
                    if (option == 0) {
                        ConnectionInstructions instructions = connection.getInstructions();
                        instructions.setAllowAutoInit(true);
                        instructions.setAllowAutoConnect(true);
                        if (connection.isAuthenticationProvided()) {
                            guarded(this, r -> r.execute());
                        } else {
                            promptAuthenticationDialog();
                        }
                    } else {
                        cancel();
                    }
                });
    }

    private void promptAuthenticationDialog() {
        ConnectionHandler connection = getConnection();
        AuthenticationInfo temporaryAuthenticationInfo = connection.getAuthenticationInfo().clone();
        temporaryAuthenticationInfo.setTemporary(true);
        ConnectionManager connectionManager = getConnectionManager(connection);
        connectionManager.promptAuthenticationDialog(
                connection,
                temporaryAuthenticationInfo,
                authenticationInfo -> {
                    if (authenticationInfo != null) {
                        guarded(() -> execute());
                    } else {
                        cancel();
                    }
                });
    }

    private void promptConnectDialog() {
        ConnectionHandler connection = getConnection();
        ConnectionManager connectionManager = getConnectionManager(connection);
        connectionManager.promptConnectDialog(
                connection,
                title,
                option -> {
                    if (option == 0) {
                        connection.getInstructions().setAllowAutoConnect(true);
                        guarded(() -> execute());
                    } else {
                        cancel();
                    }
                });
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return nd(this.context).ensureConnection();
    }



    static ConnectionManager getConnectionManager(ConnectionHandler connection) {
        return ConnectionManager.getInstance(connection.getProject());
    }

    public static void invoke(
            @DialogTitle String title,
            boolean interactive,
            DatabaseContext databaseContext,
            Consumer<ConnectionAction> action) {
        new ConnectionAction(title, interactive, databaseContext) {
            @Override
            public void execute() {
                ThreadMonitor.surround(ThreadInfo.copy(), null,
                        () -> guarded(() -> action.accept(this)));
            }

        }.start();
    }

    public static void invoke(
            @DialogTitle String title,
            boolean interactive,
            DatabaseContext databaseContext,
            Consumer<ConnectionAction> action,
            Consumer<ConnectionAction> cancel,
            Predicate<ConnectionAction> canExecute) {

        new ConnectionAction(title, interactive, databaseContext) {
            @Override
            public void execute() {
                if (canExecute == null || canExecute.test(this)) {
                    ThreadMonitor.surround(ThreadInfo.copy(), null, () -> guarded(() -> action.accept(this)));

                }
            }

            @Override
            protected void cancel() {
                super.cancel();
                if (cancel != null){
                    cancel.accept(this);
                }
            }

        }.start();
    }
}
