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

package com.dbn.object.type;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@NonNls
public enum DBTriggerEvent implements Constant<DBTriggerEvent>, Presentable {
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    TRUNCATE("TRUNCATE"),
    DROP("DROP"),
    LOGON("LOGON"),
    ALTER("ALTER"),
    CREATE("CREATE"),
    RENAME("RENAME"),
    DDL("DDL"),
    UNKNOWN("UNKNOWN");

    private final String name;

    DBTriggerEvent(String name) {
        this.name = name;
    }

    public static DBTriggerEvent value(@Nullable String value) {
        if (value == null) return UNKNOWN;

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (DBTriggerEvent triggerEvent : values()) {
            if (triggerEvent != UNKNOWN && normalized.contains(triggerEvent.name)) {
                return triggerEvent;
            }
        }
        return UNKNOWN;
    }

    public static DBTriggerEvent[] values(@Nullable String value) {
        List<DBTriggerEvent> triggerEvents = new ArrayList<>();
        if (value != null && !value.isBlank()) {
            for (String eventValue : value.split("(?i)\\s+OR\\s+")) {
                DBTriggerEvent triggerEvent = value(eventValue);
                if (triggerEvent != UNKNOWN && !triggerEvents.contains(triggerEvent)) {
                    triggerEvents.add(triggerEvent);
                }
            }
        }
        if (triggerEvents.isEmpty()) {
            triggerEvents.add(UNKNOWN);
        }
        return triggerEvents.toArray(new DBTriggerEvent[0]);
    }
}
