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

package com.dbn.connection.console.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.object.DBConsole;
import com.dbn.vfs.DBConsoleType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class CreateRenameConsoleDialog extends DBNDialog<CreateRenameConsoleForm> {
    private final ConnectionRef connection;
    private DBConsole console;
    private DBConsoleType consoleType;

    public CreateRenameConsoleDialog(ConnectionHandler connection, @NotNull DBConsoleType consoleType) {
        super(connection.getProject(), "Create " + consoleType.getName(), true);
        this.connection = connection.ref();
        this.consoleType = consoleType;
        setModal(true);
        renameAction(getOKAction(), "Create");
        init();
    }

    public CreateRenameConsoleDialog(ConnectionHandler connection, @NotNull DBConsole console) {
        super(connection.getProject(), "Rename " + console.getConsoleType().getName(), true);
        this.connection = connection.ref();
        this.console = console;
        setModal(true);
        renameAction(getOKAction(), "Rename");
        init();
    }

    @NotNull
    @Override
    protected CreateRenameConsoleForm createForm() {
        ConnectionHandler connection = this.connection.ensure();
        return console == null ?
                new CreateRenameConsoleForm(this, connection, null, consoleType) :
                new CreateRenameConsoleForm(this, connection, console, console.getConsoleType());
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
        DatabaseConsoleManager databaseConsoleManager = DatabaseConsoleManager.getInstance(getProject());
        CreateRenameConsoleForm component = getForm();
        DBConsole console = component.getConsole();
        if (console == null) {
            databaseConsoleManager.createConsole(
                    component.getConnection(),
                    component.getConsoleName(),
                    component.getConsoleType());
        } else {
            databaseConsoleManager.renameConsole(console, component.getConsoleName());
        }
        super.doOKAction();
    }

    @Override
    @NotNull
    public Action getOKAction() {
        return super.getOKAction();
    }
}
