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

package com.dbn.common.outcome;

import com.dbn.common.Priority;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.notification.NotificationCategory;
import com.dbn.common.util.UserDataHolders;
import com.intellij.openapi.project.Project;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.notification.NotificationSupport.sendErrorNotification;
import static com.dbn.common.notification.NotificationSupport.sendInfoNotification;
import static com.dbn.common.notification.NotificationSupport.sendWarningNotification;

/**
 * Generic implementation of an {@link OutcomeHandler} that produces IDE notifications using {@link com.intellij.notification.Notification} framework
 * (invokes utilities from {@link com.dbn.common.notification.NotificationSupport} in accordance with the {@link OutcomeType} of the handled {@link Outcome})
 *
 * @author Dan Cioca (Oracle)
 */
public final class NotificationOutcomeHandler extends ProjectOutcomeHandler  {
    private final NotificationCategory category;

    private NotificationOutcomeHandler(Project project, NotificationCategory category){
        super(project);
        this.category = category;
    }

    @Override
    public Priority getPriority() {
        return Priority.MEDIUM;
    }

    public static OutcomeHandler get(Project project, NotificationCategory group) {
        Map<NotificationCategory, NotificationOutcomeHandler> handlers = UserDataHolders.ensure(project, UserDataKeys.NOTIFICATION_OUTCOME_HANDLERS, () -> new ConcurrentHashMap<>());
        return handlers.computeIfAbsent(group, g -> new NotificationOutcomeHandler(project, g));
    }

    @Override
    public void handle(Outcome outcome) {
        Project project = getProject();
        String message = outcome.getMessage();

        switch (outcome.getType()) {
            case SUCCESS: sendInfoNotification(project, category, message); break;
            case WARNING: sendWarningNotification(project, category, message); break;
            case FAILURE: sendErrorNotification(project, category, message); break;
        }

    }
}
