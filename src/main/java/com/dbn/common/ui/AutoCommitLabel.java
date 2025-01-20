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

package com.dbn.common.ui;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.panel.DBNPanelImpl;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.mapping.FileConnectionContextListener;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.transaction.TransactionAction;
import com.dbn.connection.transaction.TransactionListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;

import static com.dbn.connection.ConnectionHandler.isLiveConnection;

public class AutoCommitLabel extends DBNPanelImpl implements Disposable {
    private interface Colors {
        Color DISCONNECTED = new JBColor(new Color(0x454545), new Color(0x808080));
        Color CONNECTED = new JBColor(new Color(0x454545), new Color(0x808080));
    }
    private ConnectionRef connection;
    private WeakRef<VirtualFile> virtualFile;
    private SessionId sessionId;
    private boolean subscribed = false;

    private final JLabel connectionLabel;
    private final JLabel autoCommitLabel;

    public AutoCommitLabel() {
        setLayout(new BorderLayout());
        connectionLabel = new JLabel();
        add(connectionLabel, BorderLayout.EAST);

        autoCommitLabel = new JLabel();
        autoCommitLabel.setFont(Fonts.regularBold());
        add(autoCommitLabel, BorderLayout.WEST);

        add(new JLabel(" "), BorderLayout.CENTER);

    }

    public void init(Project project, VirtualFile file, ConnectionHandler connection, DatabaseSession session) {
        init(project, file, connection, session == null ? null : session.getId());
    }

    public void init(Project project, VirtualFile file, ConnectionHandler connection, SessionId sessionId) {
        this.virtualFile = WeakRef.of(file);
        this.connection = ConnectionRef.of(connection);
        this.sessionId = Commons.nvl(sessionId, SessionId.MAIN);
        if (!subscribed) {
            subscribed = true;
            ProjectEvents.subscribe(project, this, ConnectionStatusListener.TOPIC, connectionStatusListener);
            ProjectEvents.subscribe(project, this, FileConnectionContextListener.TOPIC, connectionMappingListener);
            ProjectEvents.subscribe(project, this, TransactionListener.TOPIC, transactionListener);
        }
        update();
    }

    private void update() {
        Dispatch.run(true, () -> {
            ConnectionHandler connection = getConnection();
            if (isLiveConnection(connection)) {
                setVisible(true);
                boolean disconnected = !connection.isConnected(sessionId);
                boolean autoCommit = connection.isAutoCommit();

                connectionLabel.setForeground(disconnected ? Colors.DISCONNECTED : Colors.CONNECTED);
                DatabaseSession session = connection.getSessionBundle().getSession(sessionId);


                String sessionName = session.getName();
                connectionLabel.setText(disconnected ? " - not connected" : " - connected");
                connectionLabel.setToolTipText(
                        disconnected ?
                                "Not connected to " + sessionName + " database session" : "");

                connectionLabel.setFont(disconnected ?
                        Fonts.regular() :
                        Fonts.regularBold());

                autoCommitLabel.setForeground(autoCommit ?
                        com.dbn.common.color.Colors.FAILURE_COLOR :
                        com.dbn.common.color.Colors.SUCCESS_COLOR);
                autoCommitLabel.setText(autoCommit ? "Auto-Commit ON" : "Auto-Commit OFF");
                autoCommitLabel.setToolTipText(
                        autoCommit ?
                                "Auto-Commit is enabled for connection \"" + connection + "\". Data changes will be automatically committed to the database." :
                                "Auto-Commit is disabled for connection \"" + connection + "\". Data changes will need to be manually committed to the database.");
            } else {
                setVisible(false);
            }
        });
    }

    @Nullable
    private ConnectionHandler getConnection() {
        return ConnectionRef.get(connection);
    }

    private final ConnectionStatusListener connectionStatusListener = (connectionId, sessionId) -> {
        ConnectionHandler connection = getConnection();
        if (connection != null && connection.getConnectionId() == connectionId) {
            update();
        }
    };

    private final FileConnectionContextListener connectionMappingListener = new FileConnectionContextListener() {
        @Override
        public void connectionChanged(Project project, VirtualFile file, ConnectionHandler connection) {
            VirtualFile localVirtualFile = getVirtualFile();
            if (file.equals(localVirtualFile)) {
                AutoCommitLabel.this.connection = ConnectionRef.of(connection);
                update();
            }
        }

        @Override
        public void sessionChanged(Project project, VirtualFile file, DatabaseSession session) {
            VirtualFile localVirtualFile = getVirtualFile();
            if (file.equals(localVirtualFile)) {
                sessionId = session == null ? SessionId.MAIN : session.getId();
                update();
            }
        }
    };

    /********************************************************
     *                Transaction Listener                  *
     ********************************************************/
    private final TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void afterAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action, boolean succeeded) {
            if (action.isOneOf(
                    TransactionAction.TURN_AUTO_COMMIT_ON,
                    TransactionAction.TURN_AUTO_COMMIT_OFF) &&
                    ConnectionRef.get(AutoCommitLabel.this.connection) == connection) {

                update();
            }
        }
    };


    @Nullable
    public VirtualFile getVirtualFile() {
        return WeakRef.get(virtualFile);
    }
}
