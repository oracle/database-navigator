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

package com.dbn.event.registration;

import com.dbn.connection.ConnectionId;
import com.dbn.event.model.DatabaseChangeRegistration;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.util.containers.CollectionFactory.createConcurrentWeakValueMap;

public class EventRegistrationCache {
    private final Map<ConnectionId, Map<String, DatabaseChangeRegistration>> registrations = new ConcurrentHashMap<>();

    public void addRegistration(ConnectionId connectionId, String tableIdentifier, DatabaseChangeRegistration registration) {
        // hold week reference to registrations so they be cleaned-up if no-one else is holding the reference
        var registrations = this.registrations.computeIfAbsent(connectionId, key -> createConcurrentWeakValueMap());
        registrations.put(tableIdentifier, registration);
    }

    @Nullable
    public DatabaseChangeRegistration removeRegistration(ConnectionId connectionId, String tableName) {
        var registrations = this.registrations.get(connectionId);
        if (registrations == null) return null;

        return registrations.remove(tableName);
    }


    @Nullable
    public DatabaseChangeRegistration getRegistration(ConnectionId connectionId, String tableIdentifier) {
        var registrations = this.registrations.get(connectionId);
        if (registrations == null) return null;

        return registrations.get(tableIdentifier);
    }

    public boolean isListening(ConnectionId connectionId, String tableIdentifier) {
        return getRegistration(connectionId, tableIdentifier) != null;
    }


    public boolean isActive(ConnectionId connectionId, long regId) {
        var registrations = this.registrations.get(connectionId);
        if (registrations == null) return false;

        // TODO this is called quite often from the UI to refresh cell renderers (create weak ref cache)
        return registrations.values().stream().anyMatch(r -> r.getRegId() == regId);
    }
}
