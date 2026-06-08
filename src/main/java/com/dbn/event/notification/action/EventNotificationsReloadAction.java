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

package com.dbn.event.notification.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.event.notification.ui.EventNotificationsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.event.notification.action.EventNotificationActionUtil.getNotificationForm;
import static com.dbn.nls.NlsResources.txt;

public class EventNotificationsReloadAction extends BasicAction {

    public EventNotificationsReloadAction() {
        super(txt("app.eventNotification.action.ReloadNotifications"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EventNotificationsForm notificationForm = getNotificationForm(e);
        if (notificationForm == null) return;

        notificationForm.refresh();
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        EventNotificationsForm notificationForm = getNotificationForm(e);

        presentation.setEnabled(notificationForm != null && !notificationForm.isLoading());
        presentation.setText(txt("app.eventNotification.action.Reload"));
        presentation.setIcon(Icons.ACTION_RELOAD);
    }
}
