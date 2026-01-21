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

package com.dbn.event.registration.filter;

import com.dbn.common.filter.Filter;
import com.dbn.common.filter.FilterOption;
import com.dbn.connection.ConnectionId;
import com.dbn.event.registration.EventRegistrationCache;
import com.dbn.event.registration.EventRegistrationManager;
import com.dbn.event.registration.model.DataChangeRegistration;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.dbn.event.registration.filter.EventRegistrationFilterType.FILTER_STATUS_LISTENING;
import static com.dbn.event.registration.filter.EventRegistrationFilterType.FILTER_STATUS_NOT_LISTENING;

@Data // IMPORTANT: "hashCode" needed for the filter signature watchers
public class EventRegistrationFilter implements Filter<DataChangeRegistration> {
    private final ConnectionId connectionId;
    private FilterOption user;
    private FilterOption table;
    private FilterOption status;

    public EventRegistrationFilter(ConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public boolean accepts(DataChangeRegistration registration) {
        return
            matchesUser(registration) &&
            matchesTable(registration) &&
            matchesStatus(registration);
    }

    private boolean matchesUser(DataChangeRegistration registration) {
        return user == null || user.matchesIgnoreCase(registration.getUserName());
    }

    private boolean matchesTable(DataChangeRegistration registration) {
        return table == null || table.matchesIgnoreCase(registration.getTableName());
    }

    private boolean matchesStatus(DataChangeRegistration registration) {
        if (status == null) return true; // no filter on status

        EventRegistrationManager registrationManager = EventRegistrationManager.getInstance(registration.getProject());
        EventRegistrationCache registrationCache = registrationManager.getRegistrationCache();
        boolean active = registrationCache.isActive(connectionId, registration.getRegId());

        if (Objects.equals(FILTER_STATUS_LISTENING, status)) return active;
        if (Objects.equals(FILTER_STATUS_NOT_LISTENING, status)) return !active;

        return false;
    }

    @Nullable
    public FilterOption getFilterOption(EventRegistrationFilterType filterType) {
        return switch (filterType) {
            case USER -> user;
            case TABLE -> table;
            case STATUS -> status;
        };
    }

    public void clear() {
        user = null;
        table = null;
        status = null;
    }

    @Override
    public boolean isEmpty() {
        return user == null && table == null && status == null;
    }
}
