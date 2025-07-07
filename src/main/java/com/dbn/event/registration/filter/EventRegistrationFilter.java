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
import com.dbn.common.util.Strings;
import com.dbn.event.registration.EventRegistrationManager;
import com.dbn.event.registration.model.DataChangeRegistration;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Strings.equalsIgnoreCase;

@Data // IMPORTANT: "hashCode" needed for the filter signature watchers
public class EventRegistrationFilter implements Filter<DataChangeRegistration> {
    private String user;
    private String table;
    private String status;

    @Override
    public boolean accepts(DataChangeRegistration registration) {
        return
            matchesUser(registration) &&
            matchesTable(registration) &&
            matchesStatus(registration);
    }

    private boolean matchesUser(DataChangeRegistration registration) {
        return Strings.isEmpty(user) || equalsIgnoreCase(user, registration.getUserName());
    }

    private boolean matchesTable(DataChangeRegistration registration) {
        return Strings.isEmpty(table) || equalsIgnoreCase(table, registration.getTableName());
    }

    private boolean matchesStatus(DataChangeRegistration registration) {
        if (Strings.isEmpty(status)) return true; // no filter on status

        EventRegistrationManager registrationManager = EventRegistrationManager.getInstance(registration.getProject());
        boolean active = registrationManager.isActive(registration.getRegId());

        if (equalsIgnoreCase(status, "Active")) return active;
        if (equalsIgnoreCase(status, "Inactive")) return !active;

        return false;
    }

    @Nullable
    public String getFilterValue(EventRegistrationFilterType filterType) {
        switch (filterType) {
            case USER: return user;
            case TABLE: return table;
            case STATUS: return status;
        }
        return null;
    }
}
