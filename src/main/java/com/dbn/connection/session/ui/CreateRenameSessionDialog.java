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

package com.dbn.connection.session.ui;

import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.session.DatabaseSessionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

public class CreateRenameSessionDialog extends DBNDialog<CreateRenameSessionForm> {
    private final ConnectionRef connection;
    private WeakRef<DatabaseSession> session;

    public CreateRenameSessionDialog(ConnectionHandler connection, @Nullable DatabaseSession session) {
        super(connection.getProject(), session == null ? "Create session" : "Rename session", true);
        this.connection = connection.ref();
        this.session = WeakRef.of(session);
        renameAction(getOKAction(), session == null ? "Create" : "Rename");
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected CreateRenameSessionForm createForm() {
        ConnectionHandler connection = this.connection.ensure();
        return new CreateRenameSessionForm(this, connection, getSession());
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
            getOKAction(),
            getCancelAction()
        };
    }

    @Override
    protected void doOKAction() {
        CreateRenameSessionForm component = getForm();
        DatabaseSessionManager databaseSessionManager = DatabaseSessionManager.getInstance(getProject());
        if (session == null) {
            DatabaseSession session = databaseSessionManager.createSession(
                    component.getConnection(),
                    component.getSessionName());
            this.session = WeakRef.of(session);
            component.setSession(session);

        } else {
            databaseSessionManager.renameSession(getSession(), component.getSessionName());
        }
        super.doOKAction();
    }

    public DatabaseSession getSession() {
        return WeakRef.get(session);
    }

    @Override
    @NotNull
    public Action getOKAction() {
        return super.getOKAction();
    }
}
