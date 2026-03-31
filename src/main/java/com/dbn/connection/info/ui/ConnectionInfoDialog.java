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
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.sql.SQLException;

public class ConnectionInfoDialog extends DBNDialog<ConnectionInfoForm> {
    private ConnectionRef connection;
    private ConnectionInfo connectionInfo;
    private SQLException connectionError;
    private String connectionName;
    private EnvironmentType environmentType;

    public ConnectionInfoDialog(@NotNull ConnectionHandler connection, @Nullable ConnectionInfo connectionInfo, @Nullable SQLException connectionError) {
        super(connection.getProject(), "Connection information", true);
        this.connection = connection.ref();
        this.connectionInfo = connectionInfo;
        this.connectionError = connectionError;
        setModal(true);
        init();
    }

    public ConnectionInfoDialog(Project project, ConnectionInfo connectionInfo, String connectionName, EnvironmentType environmentType) {
        super(project, "Connection information", true);
        this.connectionInfo = connectionInfo;
        this.connectionName = connectionName;
        this.environmentType = environmentType;
        setModal(true);
        init();
    }

    @Override
    protected String getDimensionServiceKey() {
        return null;
    }

    @NotNull
    @Override
    protected ConnectionInfoForm createForm() {
        if (connection != null) {
            ConnectionHandler connection = this.connection.ensure();
            return new ConnectionInfoForm(this, connection, connectionInfo, connectionError);
        } else {
            return new ConnectionInfoForm(this, connectionInfo, connectionName, environmentType);
        }
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        renameAction(getCancelAction(), "Close");
        return actions(getCancelAction());
    }
}
