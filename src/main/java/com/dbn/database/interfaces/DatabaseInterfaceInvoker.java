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

package com.dbn.database.interfaces;

import com.dbn.common.Priority;
import com.dbn.common.cache.CacheKey;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseInterface.Callable;
import com.dbn.database.interfaces.queue.InterfaceTaskRequest;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;

import java.sql.SQLException;

import static com.dbn.database.interfaces.DatabaseInterface.ConnectionCallable;
import static com.dbn.database.interfaces.DatabaseInterface.ConnectionRunnable;

@UtilityClass
public final class DatabaseInterfaceInvoker {

    /**
     * Database Interface invocation against a pool connection
     * Schedules the task and returns immediately
     */
    public static void schedule(Priority priority, String title, String text, Project project, ConnectionId connectionId, ConnectionRunnable runnable) throws SQLException {
        InterfaceTaskRequest request = InterfaceTaskRequest.create(priority, title, text, project, connectionId, null);
        ConnectionHandler connection = request.getConnection();
        DatabaseInterfaceQueue interfaceQueue = connection.getInterfaceQueue();

        interfaceQueue.scheduleAndForget(request,
                () -> ConnectionContext.surround(request,
                        () -> PooledConnection.run(request, runnable)));
    }


    /**
     * Database Interface invocation against a pool connection
     * Schedules the task and waits for the execution
     *
     * @param priority     the priority of the task
     * @param project
     * @param connectionId the connection to be invoked against
     * @param runnable     the task to be executed
     * @throws SQLException if jdbc call fails
     */
    public static void execute(Priority priority, Project project, ConnectionId connectionId, ConnectionRunnable runnable) throws SQLException {
        execute(priority, null, null, project, connectionId, runnable);
    }

    public static void execute(Priority priority, String title, String text, Project project, ConnectionId connectionId, ConnectionRunnable runnable) throws SQLException {
        execute(priority, title, text, project, connectionId, null, runnable);
    }

    /**
     * @deprecated use {@link #execute(Priority, String, String, Project, ConnectionId, ConnectionRunnable)}
     */
    public static void execute(Priority priority, String title, String text, Project project, ConnectionId connectionId, @Deprecated SchemaId schemaId, ConnectionRunnable runnable) throws SQLException {
        InterfaceTaskRequest request = InterfaceTaskRequest.create(priority, title, text, project, connectionId, schemaId);
        ConnectionHandler connection = request.getConnection();
        DatabaseInterfaceQueue interfaceQueue = connection.getInterfaceQueue();

        ThreadInfo threadInfo = ThreadInfo.copy();
        interfaceQueue.scheduleAndWait(request,
                () -> ConnectionContext.surround(request,
                    () -> ThreadMonitor.surround(threadInfo, null,
                        () -> PooledConnection.run(request, runnable))));  }


    /**
     * Database Interface invocation against a pool connection
     * Schedules the task, waits for the execution and returns result
     *
     * @param <T>          type of the entity returned by the invocation
     * @param priority     the priority of the task
     * @param project      the project
     * @param connectionId the connection to be invoked against
     * @param callable     the task to be executed
     * @throws SQLException if jdbc call fails
     */
    public static <T> T load(Priority priority, Project project, ConnectionId connectionId, ConnectionCallable<T> callable) throws SQLException {
        return load(priority, null, null, project, connectionId, callable);
    }

    public static <T> T load(Priority priority, String title, String text, Project project, ConnectionId connectionId, ConnectionCallable<T> callable) throws SQLException {
        InterfaceTaskRequest request = InterfaceTaskRequest.create(priority, title, text, project, connectionId, null);
        ConnectionHandler connection = request.getConnection();
        DatabaseInterfaceQueue interfaceQueue = connection.getInterfaceQueue();

        ThreadInfo threadInfo = ThreadInfo.copy();
        return interfaceQueue.scheduleAndReturn(request,
                () -> ConnectionContext.surround(request,
                    () -> ThreadMonitor.surround(threadInfo, null,
                            () -> PooledConnection.call(request, callable))));
    }

    public static <T> T cached(CacheKey<T> key, Callable<T> loader) throws SQLException {
        return ConnectionHandler.local().getMetaDataCache().get(key, () -> loader.call());
    }
}
