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

package com.dbn.database.interfaces.queue;

import com.dbn.common.exception.Exceptions;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.util.Strings;
import com.dbn.common.util.TimeAware;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import static com.dbn.common.thread.ThreadMonitor.isDispatchThread;
import static com.dbn.common.thread.ThreadMonitor.isModalProcess;
import static com.dbn.common.thread.ThreadMonitor.isReadActionThread;
import static com.dbn.common.thread.ThreadMonitor.isWriteActionThread;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.FINISHED;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.NEW;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.STARTED;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@Getter
class InterfaceTask<R> implements TimeAware {
    public static final Comparator<InterfaceTask<?>> COMPARATOR = (t1, t2) -> t2.request.getPriority().compareTo(t1.request.getPriority());
    private static final long TEN_SECONDS = TimeUnit.SECONDS.toNanos(10);
    private static final long ONE_SECOND = TimeUnit.SECONDS.toNanos(1);

    @Delegate
    private final InterfaceTaskRequest request;
    private final InterfaceTaskSource source;
    private final ThrowableCallable<R, SQLException> executor;
    private final StatusHolder<InterfaceTaskStatus> status = new StatusHolder<>(NEW);

    private R response;
    private Throwable exception;

    InterfaceTask(InterfaceTaskRequest request, boolean synchronous, ThrowableCallable<R, SQLException> executor) {
        this.request = request;
        this.executor = executor;
        this.source = new InterfaceTaskSource(synchronous);
    }

    @Override
    public long getTimestamp() {
        return source.getTimestamp();
    }

    final R execute() {
        try {
            status.change(STARTED);
            this.response = executor.call();
        } catch (Throwable e) {
            conditionallyLog(e);
            this.exception = e;
        } finally {
            status.change(FINISHED);
            LockSupport.unpark(source.getThread());
        }
        return this.response;
    }

    final void awaitCompletion() throws SQLException {
        if (!source.isWaiting()) return;

        boolean validCallingThread = verifyCallingTread();
        boolean modalProcess = isModalProcess();
        while (status.isBefore(FINISHED)) {
            LockSupport.parkNanos(this, validCallingThread && !modalProcess  ? TEN_SECONDS : ONE_SECOND);

            if (ProgressMonitor.isProgressCancelled()) break;
            if (is(InterfaceTaskStatus.CANCELLED)) break;
            if (!validCallingThread) break;

            if (isOlderThan(5, TimeUnit.MINUTES)) {
                exception = new TimeoutException();
                break;
            }
        }

        if (exception == null) return;
        throw Exceptions.toSqlException(exception);
    }

    private static boolean verifyCallingTread() {
        if (isDispatchThread()) return handleIllegalCallingThread("event dispatch thread");
        if (isWriteActionThread()) return handleIllegalCallingThread("write action threads");
        if (isReadActionThread()) return handleIllegalCallingThread("read action threads");
        return true;
    }

    private static boolean handleIllegalCallingThread(@NonNls String identifier) {
        log.error("Database interface access is not allowed from {}: ThreadInfo {}", identifier,
                ThreadInfo.copy(),
                new RuntimeException("Illegal database interface invocation"));
        return false;
    }

    public boolean is(InterfaceTaskStatus status) {
        return this.status.is(status);
    }

    public boolean changeStatus(InterfaceTaskStatus status) {
        return this.status.change(status);
    }

    public boolean isProgress() {
        return Strings.isNotEmpty(getTitle());
    }
}
