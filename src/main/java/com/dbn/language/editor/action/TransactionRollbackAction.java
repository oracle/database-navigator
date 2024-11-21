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

package com.dbn.language.editor.action;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.transaction.DatabaseTransactionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class TransactionRollbackAction extends TransactionEditorAction {

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        super.update(e, project);

        Presentation presentation = e.getPresentation();
        presentation.setText("Rollback");
        presentation.setDescription("Rollback changes");
        presentation.setIcon(Icons.CONNECTION_ROLLBACK);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ConnectionHandler connection = getConnection(e);
        if (connection == null) return;

        DBNConnection conn = getTargetConnection(e);
        if (conn == null) return;

        DatabaseTransactionManager transactionManager = DatabaseTransactionManager.getInstance(project);
        transactionManager.rollback(connection, conn, true, false, null);
    }
}
