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

package com.dbn.connection.transaction.action;

import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.action.AbstractConnectionToggleAction;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class DatabaseLoggingToggleAction extends AbstractConnectionToggleAction {

    public DatabaseLoggingToggleAction(ConnectionHandler connection) {
        super(txt("app.connection.action.DatabaseLogging"), connection);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return getConnection().isLoggingEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        getConnection().setLoggingEnabled(state);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        ConnectionHandler connection = getConnection();
        DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
        boolean supportsLogging = DatabaseFeature.DATABASE_LOGGING.isSupported(connection);
        Presentation presentation = e.getPresentation();
        presentation.setVisible(supportsLogging);
        String databaseLogName = compatibility.getDatabaseLogName();
        if (Strings.isNotEmpty(databaseLogName)) {
            presentation.setText(txt("app.connection.action.DatabaseLoggingNamed", databaseLogName));
        }
    }
}
