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

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionUtil;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.event.model.DatabaseChangeRegistration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Driver;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Unsafe.warned;
import static com.intellij.util.containers.CollectionFactory.createConcurrentWeakMap;
import static java.util.Collections.emptyList;

public class EventRegistrationCache {
    // hold weak reference to registrations by using weak cache around the driver class-loader
    private final Map<ClassLoader, Map<ConnectionId, EventRegistrationData>> registrations = createConcurrentWeakMap();

    public void addRegistration(ConnectionId connectionId, DatabaseChangeRegistration registration) {
        EventRegistrationData registrationData = getRegistrationData(connectionId, true);
        if (registrationData == null) return;

        registrationData.addRegistration(registration);
    }

    public int removeRegistrations(ConnectionId connectionId, String tableIdentifier) {
        EventRegistrationData registrationData = getRegistrationData(connectionId, false);
        if (registrationData == null) return 0;

        return registrationData.removeRegistrations(tableIdentifier);
    }

    public void removeRegistration(ConnectionId connectionId, long regId) {
        EventRegistrationData registrationData = getRegistrationData(connectionId, false);
        if (registrationData == null) return;

        registrationData.removeRegistration(regId);
    }

    @NotNull
    public List<DatabaseChangeRegistration> getRegistrations(ConnectionId connectionId, String tableIdentifier) {
        EventRegistrationData registrationData = getRegistrationData(connectionId, false);
        if (registrationData == null) return emptyList();

        return registrationData.getRegistrations(tableIdentifier);
    }

    public boolean isListening(ConnectionId connectionId, String tableIdentifier) {
        EventRegistrationData registrationData = getRegistrationData(connectionId, false);
        if (registrationData == null) return false;

        return registrationData.hasRegistrations(tableIdentifier);
    }


    public boolean isActive(ConnectionId connectionId, long regId) {
        EventRegistrationData registrationData = getRegistrationData(connectionId, false);
        if (registrationData == null) return false;

        return registrationData.isRegistrationPresent(regId);
    }

    @Nullable
    private EventRegistrationData getRegistrationData(ConnectionId connectionId, boolean init) {
        ClassLoader classLoader = getDriverClassLoader(connectionId);
        if (classLoader == null) return null;

        if (init) {
            var registrations = this.registrations.computeIfAbsent(classLoader, c -> new ConcurrentHashMap<>());
            return registrations.computeIfAbsent(connectionId, k -> new EventRegistrationData(connectionId));
        } else {
            var registrations = this.registrations.get(classLoader);
            if (registrations == null) return null;

            return registrations.get(connectionId);
        }
    }

    private static ClassLoader getDriverClassLoader(ConnectionId connectionId) {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return null;

        ConnectionDatabaseSettings databaseSettings = connection.getSettings().getDatabaseSettings();
        Driver driver = warned(null, () -> ConnectionUtil.resolveDriver(databaseSettings));
        if (driver == null) return null;

        return driver.getClass().getClassLoader();
    }

}
