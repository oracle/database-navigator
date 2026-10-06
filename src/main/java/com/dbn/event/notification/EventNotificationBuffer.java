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

package com.dbn.event.notification;

import com.dbn.event.notification.model.DataChangeNotification;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EventNotificationBuffer {
    private final Deque<DataChangeNotification> events = new ArrayDeque<>();
    // This remains small in practice because events can only originate from registered tables.
    private final Map<DBObjectRef<DBDataset>, Long> latestChanges = new HashMap<>();

    synchronized void push(DataChangeNotification event) {
        latestChanges.merge(event.getTable(), event.getTimestamp(), (existing, current) -> Math.max(existing, current));

        if (events.size() >= EventNotificationData.MAX_NOTIFICATIONS_PER_CONNECTION) {
            events.removeFirst();
        }
        events.addLast(event);
    }

    @NotNull
    synchronized List<DataChangeNotification> snapshot() {
        return List.copyOf(events);
    }

    synchronized int countEventsSince(DBDataset dataset, long loadTimestamp) {
        int count = 0;
        for (DataChangeNotification event : events) {
            if (event.matches(dataset) && event.isAfter(loadTimestamp)) {
                count++;
            }
        }

        if (count > 0) return count;

        Long timestamp = latestChanges.get(dataset.ref());
        return timestamp != null && timestamp > loadTimestamp ? 1 : 0;
    }

    synchronized void clear() {
        events.clear();
        latestChanges.clear();
    }
}
