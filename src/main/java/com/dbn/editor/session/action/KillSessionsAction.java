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

package com.dbn.editor.session.action;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.ui.table.SessionBrowserTable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class KillSessionsAction extends AbstractSessionBrowserAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        SessionBrowser sessionBrowser = getSessionBrowser(e);
        if (sessionBrowser != null) {
            sessionBrowser.killSelectedSessions();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        SessionBrowser sessionBrowser = getSessionBrowser(e);
        boolean visible = false;
        boolean enabled = false;
        if (sessionBrowser != null) {
            ConnectionHandler connection = Failsafe.nn(sessionBrowser.getConnection());
            visible = DatabaseFeature.SESSION_KILL.isSupported(connection);
            SessionBrowserTable editorTable = sessionBrowser.getBrowserTable();
            enabled = editorTable.getSelectedRows().length > 0;
        }

        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.sessionBrowser.action.KillSessions"));
        presentation.setIcon(Icons.ACTION_KILL_SESSION);
        presentation.setVisible(visible);
        presentation.setEnabled(enabled);
    }
}