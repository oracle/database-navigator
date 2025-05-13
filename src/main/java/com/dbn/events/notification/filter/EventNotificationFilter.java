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

package com.dbn.events.notification.filter;

import com.dbn.common.filter.Filter;
import com.dbn.common.util.Strings;
import com.dbn.events.notification.model.DataChangeEvent;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Strings.equalsIgnoreCase;

@Data // IMPORTANT: "hashCode" needed for the filter signature watchers
public class EventNotificationFilter implements Filter<DataChangeEvent> {
    private String table;
    private String operation;

    @Override
    public boolean accepts(DataChangeEvent event) {
        return
            matchesTable(event) &&
            matchesOperation(event);
    }

    private boolean matchesTable(DataChangeEvent event) {
        return Strings.isEmpty(table) ||
                equalsIgnoreCase(table, event.getTableName());
    }

    private boolean matchesOperation(DataChangeEvent event) {
        return Strings.isEmpty(table) ||
                equalsIgnoreCase(table, event.getOperation());
    }

    @Nullable
    public String getFilterValue(EventNotificationFilterType filterType) {
        switch (filterType) {
            case TABLE: return table;
            case OPERATION: return operation;
        }
        return null;
    }
}
