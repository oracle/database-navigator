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
import com.dbn.connection.config.datasource.ui.DataSourceConfigEntryDialog;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBDataSourceConfigEntry;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.editor.ObjectEditorProvider;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.operation.DatabaseOperation.MANAGE_DATA_SOURCE_CONFIG_ENTRIES;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.DATA_SOURCE_CONFIG_ENTRY;

/**
 * {@link ObjectEditorProvider} for {@link DBDataSourceConfigEntry} (a connection-root configuration entry).
 * Owns the create/edit input dialog; the entry lifecycle runs through {@link com.dbn.object.management.ObjectManagementService}.
 */
public class DataSourceConfigEntryEditorProvider implements ObjectEditorProvider {

    @Override
    public DBObjectType getObjectType() {
        return DATA_SOURCE_CONFIG_ENTRY;
    }

    @Override
    public void openCreateDialog(DBObjectList objectList) {
        ConnectionHandler connection = objectList.getConnection();
        MANAGE_DATA_SOURCE_CONFIG_ENTRIES.start(
                connection,
                () -> Dialogs.show(() -> new DataSourceConfigEntryDialog(connection)));
    }

    @Override
    public void openEditDialog(DBObject object) {
        DBDataSourceConfigEntry entry = (DBDataSourceConfigEntry) object;
        Project project = entry.getProject();
        MANAGE_DATA_SOURCE_CONFIG_ENTRIES.start(
                entry.getConnection(),
                () -> openEditor(project, entry));
    }

    private static void openEditor(@NotNull Project project, @NotNull DBDataSourceConfigEntry entry) {
        try {
            String value = loadValue(project, entry);
            Dispatch.run((ModalityState) null, () -> Dialogs.show(() -> new DataSourceConfigEntryDialog(entry, value)));
        } catch (Exception e) {
            Dispatch.run((ModalityState) null, () -> Messages.showErrorDialog(project, txt("msg.datasource.error.ConfigEntryLoadFailed"), e));
        }
    }

    private static @NotNull String loadValue(@NotNull Project project, @NotNull DBDataSourceConfigEntry entry) throws SQLException {
        String value = DatabaseInterfaceInvoker.load(
                Priority.HIGHEST,
                txt("prc.datasource.title.LoadingConfigEntry"),
                txt("prc.datasource.text.LoadingConfigEntry", entry.getName()),
                project,
                entry.getConnectionId(),
                conn -> entry.getConnection().getDataSourceConfigInterface().loadDataSourceConfigEntryValue(entry.getName(), conn));

        if (value == null) {
            throw new SQLException(txt("msg.datasource.error.ConfigEntryNotFound", entry.getName()));
        }
        return value;
    }
}
