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

import com.dbn.connection.ConnectionId;
import com.dbn.event.notification.model.DataChangeNotification;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EventNotificationDataTest {
    @Test
    public void retainsOnlyRecentNotifications() {
        EventNotificationData data = new EventNotificationData();
        ConnectionId connectionId = ConnectionId.create();

        for (int i = 0; i <= EventNotificationData.MAX_NOTIFICATIONS_PER_CONNECTION; i++) {
            data.pushEvent(connectionId, notification(connectionId, "ROW_" + i));
        }

        List<DataChangeNotification> notifications = data.getNotifications(connectionId);
        Assert.assertEquals(EventNotificationData.MAX_NOTIFICATIONS_PER_CONNECTION, notifications.size());
        Assert.assertEquals("ROW_1", notifications.get(0).getRowId());
        Assert.assertEquals("ROW_" + EventNotificationData.MAX_NOTIFICATIONS_PER_CONNECTION,
                notifications.get(notifications.size() - 1).getRowId());

        try {
            notifications.clear();
            Assert.fail("Notification snapshots must be immutable");
        } catch (UnsupportedOperationException ignored) {
            // expected
        }
    }

    @Test
    public void concurrentPushesRemainBounded() throws Exception {
        EventNotificationData data = new EventNotificationData();
        ConnectionId connectionId = ConnectionId.create();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> pushNotifications(data, connectionId, "FIRST_"));
            Future<?> second = executor.submit(() -> pushNotifications(data, connectionId, "SECOND_"));
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        Assert.assertEquals(EventNotificationData.MAX_NOTIFICATIONS_PER_CONNECTION,
                data.getNotifications(connectionId).size());
    }

    @Test
    public void staleDetectionSurvivesNotificationEviction() {
        EventNotificationData data = new EventNotificationData();
        ConnectionId connectionId = ConnectionId.create();
        DataChangeNotification changedTable = notification(connectionId, "CHANGED_ROW");
        long loadTimestamp = changedTable.getTimestamp() - 1;

        data.pushEvent(connectionId, changedTable);
        for (int i = 0; i < EventNotificationData.MAX_NOTIFICATIONS_PER_CONNECTION; i++) {
            data.pushEvent(connectionId, notification(connectionId, "ROW_" + i, "SCHEMA.OTHER_TABLE"));
        }

        Assert.assertFalse(data.getNotifications(connectionId).contains(changedTable));
        Assert.assertEquals(1, data.countEventsSince(datasetFor(changedTable), loadTimestamp));
    }

    @Test
    public void removingConnectionNotificationsClearsStaleDetection() {
        EventNotificationData data = new EventNotificationData();
        ConnectionId connectionId = ConnectionId.create();
        DataChangeNotification notification = notification(connectionId, "ROW");
        data.pushEvent(connectionId, notification);

        data.removeNotifications(connectionId);

        Assert.assertTrue(data.getNotifications(connectionId).isEmpty());
        Assert.assertEquals(0, data.countEventsSince(datasetFor(notification), notification.getTimestamp() - 1));
    }

    private static void pushNotifications(EventNotificationData data, ConnectionId connectionId, String prefix) {
        for (int i = 0; i < EventNotificationData.MAX_NOTIFICATIONS_PER_CONNECTION; i++) {
            data.pushEvent(connectionId, notification(connectionId, prefix + i));
        }
    }

    private static DataChangeNotification notification(ConnectionId connectionId, String rowId) {
        return notification(connectionId, rowId, "SCHEMA.TABLE");
    }

    private static DataChangeNotification notification(ConnectionId connectionId, String rowId, String tableIdentifier) {
        return new DataChangeNotification("INSERT", tableIdentifier, rowId, 1L, connectionId);
    }

    private static DBDataset datasetFor(DataChangeNotification notification) {
        DBObjectRef<DBDataset> table = notification.getTable();
        ConnectionId connectionId = notification.getConnectionId();
        return (DBDataset) Proxy.newProxyInstance(
                DBDataset.class.getClassLoader(),
                new Class<?>[]{DBDataset.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getConnectionId" -> connectionId;
                    case "ref" -> table;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
