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

import com.dbn.common.icon.Icons;
import com.dbn.editor.session.SessionBrowser;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class ReloadSessionsAction extends AbstractSessionBrowserAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        SessionBrowser sessionBrowser = getSessionBrowser(e);
        if (sessionBrowser != null) {
            sessionBrowser.loadSessions(true);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        SessionBrowser sessionBrowser = getSessionBrowser(e);
        presentation.setEnabled(sessionBrowser != null && !sessionBrowser.isLoading());
        presentation.setText("Reload");
        presentation.setIcon(Icons.DATA_EDITOR_RELOAD_DATA);
        presentation.setEnabled(true);

    }
}