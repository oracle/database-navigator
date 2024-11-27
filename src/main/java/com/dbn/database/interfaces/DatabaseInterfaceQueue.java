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

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.queue.InterfaceCounters;
import com.dbn.database.interfaces.queue.InterfaceTaskRequest;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface DatabaseInterfaceQueue extends StatefulDisposable {
    @NotNull
    ConnectionHandler getConnection();

    int size();

    int maxActiveTasks();

    InterfaceCounters counters();

    <R> R scheduleAndReturn(InterfaceTaskRequest request, ThrowableCallable<R, SQLException> callable) throws SQLException;

    void scheduleAndWait(InterfaceTaskRequest request, ThrowableRunnable<SQLException> runnable) throws SQLException;

    void scheduleAndForget(InterfaceTaskRequest request, ThrowableRunnable<SQLException> runnable) throws SQLException;
}
