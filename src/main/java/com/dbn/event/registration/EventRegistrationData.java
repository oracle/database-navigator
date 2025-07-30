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
import com.intellij.openapi.util.Predicates;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class EventRegistrationData {
    private final ConnectionId connectionId;
    private final Map<Long, DatabaseChangeRegistration> data = new HashMap<>();
    private final Map<String, Set<Long>> cache = new HashMap<>();

    public EventRegistrationData(ConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    public synchronized void addRegistration(DatabaseChangeRegistration registration) {
        long regId = registration.getRegId();
        data.put(regId, registration);

        for (String table : registration.getTables()) {
            Set<Long> registrationIds = cache.computeIfAbsent(table, t -> new LinkedHashSet<>());
            registrationIds.add(regId);
        }
    }

    public synchronized int removeRegistrations(String tableIdentifier) {
        Set<Long> regIds = cache.remove(tableIdentifier);
        if (regIds == null) return 0;

        int removed = 0;
        for (Long regId : regIds) {
            data.remove(regId);
            removed++;
        }
        return removed;
    }

    public synchronized void removeRegistration(long regId) {
        DatabaseChangeRegistration registration = getRegistration(regId);
        if (registration == null) return;

        removeRegistration(registration);
    }

    public synchronized void removeRegistration(DatabaseChangeRegistration registration) {
        long regId = registration.getRegId();
        data.remove(regId);
        for (String table : registration.getTables()) {
            Set<Long> tableCache = cache.get(table);
            if (tableCache != null) {
                tableCache.remove(regId);
            }
        }
    }

    @Nullable
    public synchronized DatabaseChangeRegistration getRegistration(long regId) {
        return data.get(regId);
    }

    public synchronized List<DatabaseChangeRegistration> getRegistrations(String tableIdentifier) {
        Set<Long> registrationIds = cache.get(tableIdentifier);
        if (registrationIds == null) return Collections.emptyList();

        return registrationIds.
                stream().
                map(id -> data.get(id)).
                filter(Predicates.nonNull()).
                collect(Collectors.toList());
    }

    public synchronized boolean hasRegistrations(String tableIdentifier) {
        Set<Long> refIds = cache.get(tableIdentifier);
        return refIds != null && !refIds.isEmpty();
    }

    public boolean isRegistrationPresent(long regId) {
        return getRegistration(regId) != null;
    }
}
