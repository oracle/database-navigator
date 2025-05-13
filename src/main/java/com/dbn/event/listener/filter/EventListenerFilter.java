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

package com.dbn.event.listener.filter;

import com.dbn.common.filter.Filter;
import com.dbn.common.util.Strings;
import com.dbn.event.listener.EventListenerManager;
import com.dbn.event.listener.model.DataChangeListener;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Strings.equalsIgnoreCase;

@Data // IMPORTANT: "hashCode" needed for the filter signature watchers
public class EventListenerFilter implements Filter<DataChangeListener> {
    private String user;
    private String table;
    private String status;

    @Override
    public boolean accepts(DataChangeListener listener) {
        return
            matchesUser(listener) &&
            matchesTable(listener) &&
            matchesStatus(listener);
    }

    private boolean matchesUser(DataChangeListener registration) {
        return Strings.isEmpty(user) || equalsIgnoreCase(user, registration.getUserName());
    }

    private boolean matchesTable(DataChangeListener registration) {
        return Strings.isEmpty(table) || equalsIgnoreCase(table, registration.getTableName());
    }

    private boolean matchesStatus(DataChangeListener registration) {
        if (Strings.isEmpty(status)) return true; // no filter on status

        EventListenerManager instance = EventListenerManager.getInstance();
        boolean active = instance.isActive(registration.getRegId());

        if (equalsIgnoreCase(status, "Active")) return active;
        if (equalsIgnoreCase(status, "Inactive")) return !active;

        return false;
    }

    @Nullable
    public String getFilterValue(EventListenerFilterType filterType) {
        switch (filterType) {
            case USER: return user;
            case TABLE: return table;
            case STATUS: return status;
        }
        return null;
    }
}
