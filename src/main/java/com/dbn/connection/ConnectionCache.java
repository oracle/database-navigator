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

import com.dbn.common.project.Projects;
import com.dbn.common.ref.WeakRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;

final class ConnectionCache {
    private static volatile Wrapper[] data = new Wrapper[50];
    private static final Lock lock = new ReentrantLock();

    private ConnectionCache() {}

    @Nullable
    public static ConnectionHandler resolve(@Nullable ConnectionId connectionId) {
        if (connectionId == null) return null;
        ensure(connectionId);
        return data(connectionId.index());
    }

    private static void ensure(ConnectionId connectionId) {
        int index = connectionId.index();
        if (found(index)) return;

        try {
            lock.lock();
            if (found(index)) return;

            // ensure capacity
            if (data.length <= index) {
                Wrapper[] oldData = data;
                Wrapper[] newData = new Wrapper[oldData.length * 2];
                System.arraycopy(oldData, 0, newData, 0, oldData.length);
                data = newData;
            } else {
                if (data[index] != null && isNotValid(data(index))) {
                    data[index] = null;
                }
            }

            // ensure entry
            for (Project project : Projects.getOpenProjects()) {
                ConnectionManager connectionManager = ConnectionManager.getInstance(project);
                ConnectionHandler connection = connectionManager.getConnection(connectionId);

                // cache as null if disposed
                if (isNotValid(connection)) connection = null;

                data[index] = new Wrapper(connection);
            }
        } finally {
            lock.unlock();
        }
    }

    private static boolean found(int index) {
        return isValid(data(index));
    }

    @Nullable
    private static ConnectionHandler data(int index) {
        Wrapper[] data = ConnectionCache.data;
        if (data.length <= index) return null;

        Wrapper wrapper = data[index];
        if (wrapper == null) return null;

        return wrapper.get();
    }

    public static void releaseCache(@NotNull Project project) {
        if (project.isDefault()) return;

        for (int i = 0; i < data.length; i++) {
            Wrapper wrapper = data[i];
            if (wrapper == null) continue;

            ConnectionHandler connectionHandler = wrapper.get();
            if (connectionHandler == null || connectionHandler.isDisposed() || connectionHandler.getProject() == project) {
                data[i] = null;
            }
        }
    }

    private static class Wrapper extends WeakRef<ConnectionHandler> {
        protected Wrapper(ConnectionHandler referent) {
            super(referent);
        }

        @Override
        public @NonNls String toString() {
            ConnectionHandler connection = get();
            return connection == null ? "null" : connection.getName();
        }
    }
}
