/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.connection;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.concurrent.Callable;

@UtilityClass
public class ConnectionIdContext {
    private static final ThreadLocal<ConnectionId> CONNECTION_ID = new ThreadLocal<>();

    public static ConnectionId get() {
        return CONNECTION_ID.get();
    }

    @SneakyThrows
    public static <T> T surround(ConnectionId connectionId, Callable<T> callable) {
        boolean initialized = init(connectionId);
        try {
            return callable.call();
        } finally {
            release(initialized);
        }
    }

     @SneakyThrows
     public static void surround(ConnectionId connectionId, Runnable runnable) {
        boolean initialized = init(connectionId);
        try {
            runnable.run();
        } finally {
            release(initialized);
        }
    }

    private static boolean init(ConnectionId connectionId) {
        ConnectionId localConnectionId = CONNECTION_ID.get();
        if (localConnectionId == null) {
            CONNECTION_ID.set(connectionId);
            return true;
        }

        if (!Objects.equals(connectionId, localConnectionId)) {
            throw new IllegalStateException("Context already initialized for another connection");
        }
        return false;
    }

    private static void release(boolean initialized) {
        if (!initialized) return;
        CONNECTION_ID.remove();
    }
}
