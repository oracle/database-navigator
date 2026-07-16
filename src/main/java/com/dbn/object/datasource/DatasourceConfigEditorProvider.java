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

package com.dbn.object.datasource;

import com.dbn.common.Priority;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatasourceConfigCreationScope;
import com.dbn.object.DBDatasourceConfig;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.datasource.ui.DatasourceConfigEditDialog;
import com.dbn.object.editor.ObjectEditorProvider;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.operation.DatabaseOperation.CREATE_DATASOURCE_CONFIG;
import static com.dbn.common.operation.DatabaseOperation.MANAGE_DATASOURCE_CONFIGS;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.DATASOURCE_CONFIG;

/**
 * {@link ObjectEditorProvider} for {@link DBDatasourceConfig} (a connection-root configuration).
 * Owns the create/edit input dialog; the entry lifecycle runs through {@link com.dbn.object.management.ObjectManagementService}.
 */
public class DatasourceConfigEditorProvider implements ObjectEditorProvider {

    @Override
    public DBObjectType getObjectType() {
        return DATASOURCE_CONFIG;
    }

    @Override
    public void openCreateDialog(DBObjectList objectList) {
        ConnectionHandler connection = objectList.getConnection();
        CREATE_DATASOURCE_CONFIG.start(
                connection,
                () -> openCreateDialog(connection));
    }

    private static void openCreateDialog(@NotNull ConnectionHandler connection) {
        try {
            DatasourceConfigCreationScope scope = DatabaseInterfaceInvoker.load(
                    Priority.HIGH,
                    connection.getProject(),
                    connection.getConnectionId(),
                    conn -> connection.getDatasourceConfigInterface().loadDatasourceConfigCreationScope(conn));
            Dialogs.show(() -> new DatasourceConfigEditDialog(connection, scope == DatasourceConfigCreationScope.ANY_SCHEMA));
        } catch (SQLException e) {
            Messages.showErrorDialog(connection.getProject(), txt("msg.datasourceConfig.error.LoadFailed"), e);
        }
    }

    @Override
    public void openEditDialog(DBObject object) {
        DBDatasourceConfig entry = (DBDatasourceConfig) object;
        Project project = entry.getProject();
        MANAGE_DATASOURCE_CONFIGS.start(
                entry.getConnection(),
                () -> openEditor(project, entry));
    }

    private static void openEditor(@NotNull Project project, @NotNull DBDatasourceConfig entry) {
        try {
            String value = loadValue(project, entry);
            Dispatch.run((ModalityState) null, () -> Dialogs.show(() -> new DatasourceConfigEditDialog(entry, value)));
        } catch (Exception e) {
            Dispatch.run((ModalityState) null, () -> Messages.showErrorDialog(project, txt("msg.datasourceConfig.error.LoadFailed"), e));
        }
    }

    private static @NotNull String loadValue(@NotNull Project project, @NotNull DBDatasourceConfig entry) throws SQLException {
        String value = DatabaseInterfaceInvoker.load(
                Priority.HIGHEST,
                txt("prc.datasourceConfig.title.Loading"),
                txt("prc.datasourceConfig.text.Loading", entry.getName()),
                project,
                entry.getConnectionId(),
                conn -> entry.getConnection().getDatasourceConfigInterface().loadDatasourceConfigValue(
                        entry.getSchema().getName(),
                        entry.getName(),
                        conn));

        if (value == null) {
            throw new SQLException(txt("msg.datasourceConfig.error.NotFound", entry.getQualifiedName()));
        }
        return value;
    }
}
