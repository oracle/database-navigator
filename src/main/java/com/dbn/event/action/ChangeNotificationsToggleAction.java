/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.event.action;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionId;
import com.dbn.event.registration.EventRegistrationCache;
import com.dbn.event.registration.EventRegistrationManager;
import com.dbn.object.DBTable;
import com.dbn.object.action.AnObjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class ChangeNotificationsToggleAction extends AnObjectAction<DBTable> {

    public ChangeNotificationsToggleAction(@NotNull DBTable table) {
        super(table);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBTable table) {
        boolean listening = isListening(table);

        EventRegistrationManager registrationManager = EventRegistrationManager.getInstance(project);
        if (listening) {
            registrationManager.unregisterTable(table);
        } else {
            registrationManager.registerTable(table);
        }

    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBTable table) {
        if (table == null) return;

        boolean listening = isListening(table);
        presentation.setText(listening ?
                txt("app.objects.action.DisableDataChangeNotifications") :
                txt("app.objects.action.EnableDataChangeNotifications"));
        presentation.setIcon(listening ?
                Icons.TABLE_DISABLE_DCN:
                Icons.TABLE_ENABLE_DCN);
    }

    private boolean isListening(@NotNull DBTable table) {
        Project project = table.getProject();
        EventRegistrationManager registrationManager = EventRegistrationManager.getInstance(project);

        String tableName = table.getQualifiedName();
        ConnectionId connectionId = table.getConnectionId();

        EventRegistrationCache registrationCache = registrationManager.getRegistrationCache();
        return registrationCache.isListening(connectionId, tableName);
    }
}
