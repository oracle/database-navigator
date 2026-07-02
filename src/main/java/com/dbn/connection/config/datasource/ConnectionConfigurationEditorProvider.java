/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.connection.config.datasource;

import com.dbn.common.Priority;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.datasource.ui.ConnectionConfigurationDialog;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBConnectionConfiguration;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.editor.ObjectEditorProvider;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.operation.DatabaseOperation.CREATE_CONNECTION_CONFIGURATION;
import static com.dbn.common.operation.DatabaseOperation.MANAGE_CONNECTION_CONFIGURATIONS;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.CONNECTION_CONFIGURATION;

/**
 * {@link ObjectEditorProvider} for {@link DBConnectionConfiguration} (a connection-root configuration).
 * Owns the create/edit input dialog; the entry lifecycle runs through {@link com.dbn.object.management.ObjectManagementService}.
 */
public class ConnectionConfigurationEditorProvider implements ObjectEditorProvider {

    @Override
    public DBObjectType getObjectType() {
        return CONNECTION_CONFIGURATION;
    }

    @Override
    public void openCreateDialog(DBObjectList objectList) {
        ConnectionHandler connection = objectList.getConnection();
        CREATE_CONNECTION_CONFIGURATION.start(
                connection,
                () -> Dialogs.show(() -> new ConnectionConfigurationDialog(connection)));
    }

    @Override
    public void openEditDialog(DBObject object) {
        DBConnectionConfiguration entry = (DBConnectionConfiguration) object;
        Project project = entry.getProject();
        MANAGE_CONNECTION_CONFIGURATIONS.start(
                entry.getConnection(),
                () -> openEditor(project, entry));
    }

    private static void openEditor(@NotNull Project project, @NotNull DBConnectionConfiguration entry) {
        try {
            String value = loadValue(project, entry);
            Dispatch.run((ModalityState) null, () -> Dialogs.show(() -> new ConnectionConfigurationDialog(entry, value)));
        } catch (Exception e) {
            Dispatch.run((ModalityState) null, () -> Messages.showErrorDialog(project, txt("msg.connectionConfig.error.LoadFailed"), e));
        }
    }

    private static @NotNull String loadValue(@NotNull Project project, @NotNull DBConnectionConfiguration entry) throws SQLException {
        String value = DatabaseInterfaceInvoker.load(
                Priority.HIGHEST,
                txt("prc.connectionConfig.title.Loading"),
                txt("prc.connectionConfig.text.Loading", entry.getName()),
                project,
                entry.getConnectionId(),
                conn -> entry.getConnection().getConnectionConfigurationInterface().loadConnectionConfigurationValue(
                        entry.getOwnerName(),
                        entry.getConfigName(),
                        conn));

        if (value == null) {
            throw new SQLException(txt("msg.connectionConfig.error.NotFound", entry.getQualifiedConfigName()));
        }
        return value;
    }
}
