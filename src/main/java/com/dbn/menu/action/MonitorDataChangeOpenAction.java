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

package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.event.notification.EventNotificationManager;
import com.dbn.options.general.WorkspaceFeature;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.database.DatabaseFeature.DATA_CHANGE_NOTIFICATION;
import static com.dbn.nls.NlsResources.txt;

public class MonitorDataChangeOpenAction extends ProjectAction {
    public MonitorDataChangeOpenAction() {
        super(txt("app.menu.action.DataEventsMonitor"));
    }

    @Override
    protected WorkspaceFeature getFeature() {
        return WorkspaceFeature.EVENT_MONITOR;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance(project);
        eventNotificationManager.showEventNotificationConsole();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean visible = isVisible(project);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(visible);
    }

    private boolean isVisible(@NotNull Project project) {
        return isFeatureEnabled(project) &&
                DATA_CHANGE_NOTIFICATION.isSupported(project);
    }
}
