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

package com.dbn.debugger.jdbc;

import com.dbn.database.interfaces.DatabaseInterface.Callable;
import com.dbn.database.interfaces.DatabaseInterface.Runnable;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes short debugger-connection operations performed while a session is suspended.
 * A suspension ID identifies one paused state and prevents queued work from reading a later frame.
 */
final class DBJdbcDebuggerInspection {
    private static final long NO_SUSPENSION_ID = 0;

    private final ReentrantLock lock = new ReentrantLock(true);
    private volatile long activeSuspensionId = NO_SUSPENSION_ID;
    private long lastSuspensionId;

    long activateSuspension() {
        lock.lock();
        try {
            activeSuspensionId = ++lastSuspensionId;
            return activeSuspensionId;
        } finally {
            lock.unlock();
        }
    }

    void invalidateSuspension() {
        activeSuspensionId = NO_SUSPENSION_ID;
    }

    void awaitCompletion() {
        lock.lock();
        try {
            // Acquiring the lock is sufficient to wait for the active inspection.
        } finally {
            lock.unlock();
        }
    }

    boolean isActiveSuspension(long suspensionId) {
        return suspensionId != NO_SUSPENSION_ID && suspensionId == activeSuspensionId;
    }

    <T> T execute(Callable<T> callable) throws SQLException {
        lock.lock();
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    <T> T call(long suspensionId, Callable<T> callable) throws SQLException {
        lock.lock();
        try {
            if (!isActiveSuspension(suspensionId)) return null;

            T result = callable.call();
            return isActiveSuspension(suspensionId) ? result : null;
        } finally {
            lock.unlock();
        }
    }

    boolean run(Runnable runnable) throws SQLException {
        long suspensionId = activeSuspensionId;
        lock.lock();
        try {
            if (!isActiveSuspension(suspensionId)) return false;

            runnable.run();
            return isActiveSuspension(suspensionId);
        } finally {
            lock.unlock();
        }
    }
}
