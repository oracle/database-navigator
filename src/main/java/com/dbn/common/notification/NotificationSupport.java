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

package com.dbn.common.notification;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.common.project.ProjectSupplier;
import com.dbn.common.util.Titles;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public interface NotificationSupport extends ProjectSupplier {

    default void sendNotification(NotificationType type, NotificationCategory category, @Nls String message) {
        sendNotification(getProject(), type, category, message);
    }

    default void sendInfoNotification(NotificationCategory category, @Nls String message) {
        sendInfoNotification(getProject(), category, message);
    }

    default void sendWarningNotification(NotificationCategory category, @Nls String message) {
        sendWarningNotification(getProject(), category, message);
    }

    default void sendErrorNotification(NotificationCategory category, @Nls String message) {
        sendErrorNotification(getProject(), category, message);
    }


    static void sendInfoNotification(@Nullable Project project, NotificationCategory category, @Nls String message) {
        sendNotification(project, NotificationType.INFORMATION, category, message);
    }

    static void sendWarningNotification(@Nullable Project project, NotificationCategory category, @Nls String message) {
        sendNotification(project, NotificationType.WARNING, category, message);
    }

    static void sendErrorNotification(@Nullable Project project, NotificationCategory category, @Nls String message) {
        sendNotification(project, NotificationType.ERROR, category, message);
    }

    static void sendNotification(@Nullable Project project, NotificationType type, NotificationCategory category, @Nls String message) {
        if (project != null && project.isDisposed()) return;

        NotificationGroup notificationGroup = category.getGroup();
        String notificationGroupId = notificationGroup.getId();
        if (usePinnedNotification(project, notificationGroupId)) notificationGroup = NotificationGroup.PINNED;

        Notification notification = new Notification(
                notificationGroup.getId(),
                Titles.signed(txt(notificationGroup.getTitleKey())),
                message,
                type);
        notification.setImportant(false);
        Notifications.Bus.notify(notification, project);
    }

    private static boolean usePinnedNotification(Project project, String notificationGroupId) {
        if (project == null) return false;

        com.intellij.notification.NotificationGroup notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(notificationGroupId);
        if (notificationGroup == null) return false;
        if (notificationGroup.getDisplayType() != NotificationDisplayType.BALLOON) return false;

        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        if (!assistantManager.divertNotificationBalloon()) return false;

        return true;
    }
}
