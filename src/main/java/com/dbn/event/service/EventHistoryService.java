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

package com.dbn.event.service;

import com.dbn.event.listener.EventListenerManager;
import com.dbn.event.notification.model.DataChangeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EventHistoryService {
  private static final EventHistoryService INSTANCE = new EventHistoryService();

  private final Map<String, Map<Long, List<DataChangeEvent>>> eventHistory = new HashMap<>();
  private final Map<String ,List<Consumer<DataChangeEvent>>> listeners = new HashMap<>();

  private EventHistoryService() { }

  public static EventHistoryService getInstance() {
    return INSTANCE;
  }

  // Push event for a specific ConnectionId and regId
  public synchronized void pushEvent(String connectionId, long regId, DataChangeEvent event) {
    eventHistory
            .computeIfAbsent(connectionId, k -> new HashMap<>())
            .computeIfAbsent(regId, k -> new ArrayList<>())
            .add(event);
    listeners.
            computeIfAbsent(connectionId, k -> new ArrayList<>())
                    .forEach(listener -> {
//                      if (connectionId.equals(event.getConnectionId())) {
                        listener.accept(event);
//                      }
                    });
//    listeners.forEach(listener -> listener.accept(event));
  }

  // Retrieve events for a specific ConnectionId and regId
  public synchronized List<DataChangeEvent> getEventsByConnectionAndRegId(String connectionId, long regId) {
    return new ArrayList<>(eventHistory
            .getOrDefault(connectionId, Collections.emptyMap())
            .getOrDefault(regId, Collections.emptyList()));
  }

  public List<DataChangeEvent> getAllEventsForConnection(String connectionId) {

    // Fetch events for the given connectionId from EventHistoryService
    Map<Long, List<DataChangeEvent>> allEvents = eventHistory.get(connectionId);


    // Check if the registrations exist, then flatten the events into a single list using streams
    if (allEvents != null) {
      return allEvents.values().stream() // Stream of List<DataChangeEvent>
              .flatMap(List::stream) // Flatten each List<DataChangeEvent> into a Stream<DataChangeEvent>
              .collect(Collectors.toList()); // Collect into a List<DataChangeEvent>
    }

    // Return an empty list if no events are found for the given connectionId
    return List.of();
  }


  public List<DataChangeEvent> getAllEventsForConnection(String connectionId, String tableNameFilter, String regStatusFilter) {
    List<DataChangeEvent> filteredEvents = new ArrayList<>();

    // Fetch all events for the given connection
    List<DataChangeEvent> allEvents = getAllEventsForConnection(connectionId);

    for (DataChangeEvent event : allEvents) {
      // Filter by table name
      if (!"All".equals(tableNameFilter) && !event.getTableName().equalsIgnoreCase(tableNameFilter)) {
        continue;
      }

      // Filter by registration status (Active/Inactive/All)
      if ("Active".equals(regStatusFilter) && !isActive(event)) {
        continue;
      } else if ("Inactive".equals(regStatusFilter) && isActive(event)) {
        continue;
      }

      // If event passes all filters, add to the list
      filteredEvents.add(event);
    }

    return filteredEvents;
  }

  public void registerListener(String connectionId,Consumer<DataChangeEvent> listener) {
   listeners.computeIfAbsent(connectionId, k -> new CopyOnWriteArrayList<>())
   .add(listener);
  }

  private boolean isActive(DataChangeEvent event) {
    // Implement the logic for determining if the event is active or inactive
    // This depends on how you define an active event in your system
    // For example, you could check if the registration ID is in the active registrations list
//    return re.contains(event.getRegistrationId());
    return EventListenerManager.getInstance().isActive(event.getRegID());
  }

  }

