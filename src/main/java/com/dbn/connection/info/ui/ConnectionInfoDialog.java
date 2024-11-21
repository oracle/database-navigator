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

package com.dbn.connection.info.ui;

import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.info.ConnectionInfo;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class ConnectionInfoDialog extends DBNDialog<ConnectionInfoForm> {
    private ConnectionRef connection;
    private ConnectionInfo connectionInfo;
    private String connectionName;
    private EnvironmentType environmentType;

    public ConnectionInfoDialog(@NotNull ConnectionHandler connection) {
        super(connection.getProject(), "Connection information", true);
        this.connection = connection.ref();
        renameAction(getCancelAction(), "Close");
        setResizable(false);
        setModal(true);
        init();
    }

    public ConnectionInfoDialog(Project project, ConnectionInfo connectionInfo, String connectionName, EnvironmentType environmentType) {
        super(project, "Connection information", true);
        this.connectionInfo = connectionInfo;
        this.connectionName = connectionName;
        this.environmentType = environmentType;
        renameAction(getCancelAction(), "Close");
        setResizable(false);
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected ConnectionInfoForm createForm() {
        if (connection != null) {
            ConnectionHandler connection = this.connection.ensure();
            return new ConnectionInfoForm(this, connection);
        } else {
            return new ConnectionInfoForm(this, connectionInfo, connectionName, environmentType);
        }
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
            getCancelAction()
        };
    }
}
