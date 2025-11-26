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

package com.dbn.event.notification;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.ConnectionId;
import com.dbn.event.notification.model.DataChangeNotification;
import com.dbn.event.notification.model.DataChangeNotificationListener;
import com.dbn.object.DBDataset;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;

public class EventNotificationData extends StatefulDisposableBase {
    private final Map<ConnectionId, List<DataChangeNotification>> notifications = new ConcurrentHashMap<>();
    private final Listeners<DataChangeNotificationListener> listeners = Listeners.create(this);

    public void pushEvent(ConnectionId connectionId, DataChangeNotification event) {
        List<DataChangeNotification> notifications = ensureNotifications(connectionId);
        notifications.add(event);
        listeners.notify(l -> l.accept(event));
    }

    @NotNull
    private List<DataChangeNotification> ensureNotifications(ConnectionId connectionId) {
        return notifications.computeIfAbsent(connectionId, k -> new CopyOnWriteArrayList<>());
    }

    public List<DataChangeNotification> getNotifications(ConnectionId connectionId) {
        List<DataChangeNotification> notifications = this.notifications.get(connectionId);
        return notifications == null ? emptyList() : notifications;
    }

    public void registerListener(ConnectionId connectionId, DataChangeNotificationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void disposeInner() {
        Disposer.disposeMap(notifications);
    }

    public int countEventsSince(DBDataset dataset, long loadTimestamp) {
        int count = 0;

        ConnectionId connectionId = dataset.getConnectionId();
        List<DataChangeNotification> notifications = getNotifications(connectionId);
        for (DataChangeNotification notification : notifications) {
            if (!notification.matches(dataset)) continue;
            if (!notification.isAfter(loadTimestamp)) continue;

            count++;
        }

        return count;
    }
}

