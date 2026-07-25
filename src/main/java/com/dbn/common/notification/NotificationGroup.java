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

package com.dbn.common.notification;

import com.dbn.common.index.Identifiable;
import lombok.Getter;

@Getter
public enum NotificationGroup implements Identifiable<String> {
    DEFAULT("DBNavigator.NotificationGroup.Default", "ntf.shared.title.DBNavigator"),
    PINNED("DBNavigator.NotificationGroup.Pinned", "ntf.shared.title.DBNavigatorPinned"),
    BROWSER("DBNavigator.NotificationGroup.Browser", "ntf.shared.title.DBNavigatorBrowser"),
    EXECUTION("DBNavigator.NotificationGroup.Execution", "ntf.shared.title.DBNavigatorExecution"),
    ASSISTANT("DBNavigator.NotificationGroup.Assistant", "ntf.shared.title.DBNavigatorAssistant"),
    DIAGNOSTICS("DBNavigator.NotificationGroup.Diagnostics", "ntf.shared.title.DBNavigatorDiagnostics"),
    EVENTS("DBNavigator.NotificationGroup.Events", "ntf.shared.title.DBNavigatorEvents");
    ;

    private final String id;
    private final String titleKey;

    NotificationGroup(String id, String titleKey) {
        this.id = id;
        this.titleKey = titleKey;
    }
}
