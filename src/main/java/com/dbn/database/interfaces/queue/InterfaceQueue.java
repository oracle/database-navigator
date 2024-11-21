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

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.routine.Consumer;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.common.thread.Threads;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.database.interfaces.DatabaseInterfaceQueue;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.LockSupport;

import static com.dbn.common.load.ProgressMonitor.isProgressThread;
import static com.dbn.database.interfaces.queue.InterfaceTask.COMPARATOR;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.CANCELLED;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.DEQUEUED;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.FINISHED;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.QUEUED;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.RELEASED;
import static com.dbn.database.interfaces.queue.InterfaceTaskStatus.SCHEDULED;

@Slf4j
public class InterfaceQueue extends StatefulDisposableBase implements DatabaseInterfaceQueue {
    private static final ExecutorService MONITORS = Threads.newCachedThreadPool("DBN - Database Interface Monitor", true);

    private final BlockingQueue<InterfaceTask<?>> queue = new PriorityBlockingQueue<>(11, COMPARATOR);
    private final Consumer<InterfaceTask<?>> consumer;
    private final InterfaceCounters counters = new InterfaceCounters();
    private final ConnectionRef connection;
    private volatile Thread monitor;

    public InterfaceQueue(ConnectionHandler connection) {
        this(connection, null);
    }

    InterfaceQueue(@Nullable ConnectionHandler connection, Consumer<InterfaceTask<?>> consumer) {
        super(connection);
        this.connection = ConnectionRef.of(connection);
        this.consumer = consumer == null ? new InterfaceQueueConsumer(this) : consumer;
        this.counters.running().addListener(value -> warnTaskLimits());

        MONITORS.submit(() -> monitorQueue());
    }

    private void warnTaskLimits() {
        if (counters.running().get() > maxActiveTasks()) {
            log.warn("Active task limit exceeded: {} (expected max {})", counters.running(), maxActiveTasks());
        }
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection(){
        return connection.ensure();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public InterfaceCounters counters() {
        return counters;
    }

    private boolean maxActiveTasksExceeded() {
        return counters.running().get() >= maxActiveTasks();
    }

    @Override
    public int maxActiveTasks() {
        if (connection == null || isDisposed()) return 10;
        return getConnection().getSettings().getDetailSettings().getMaxConnectionPoolSize();
    }

    @Override
    public <R> R scheduleAndReturn(InterfaceTaskRequest request, ThrowableCallable<R, SQLException> callable) throws SQLException {
        return queue(request, true, callable).getResponse();
    }

    @Override
    public void scheduleAndWait(InterfaceTaskRequest request, ThrowableRunnable<SQLException> runnable) throws SQLException {
        queue(request, true, ThrowableCallable.from(runnable));
    }

    @Override
    public void scheduleAndForget(InterfaceTaskRequest request, ThrowableRunnable<SQLException> runnable) throws SQLException {
        queue(request, false, ThrowableCallable.from(runnable));
    }

    @NotNull
    private <T> InterfaceTask<T> queue(InterfaceTaskRequest request, boolean synchronous, ThrowableCallable<T, SQLException> callable) throws SQLException {
        InterfaceTask<T> task = new InterfaceTask<>(request, synchronous, callable);
        try {
            queue.add(task);
            counters.queued().increment();
            task.changeStatus(QUEUED);

            task.awaitCompletion();
            return task;
        } finally {
            task.changeStatus(RELEASED);
        }
    }

    /**
     * Start monitoring the queue
     */
    @SneakyThrows
    private void monitorQueue() {
        monitor = Thread.currentThread();

        while (!isDisposed()) {
            checkDisposed();
            parkMonitor();

            InterfaceTask<?> task = queue.take();

            // increment "running" as early as possible (decision whether to park or unpark the monitor depends on the running counter)
            counters.running().increment();
            counters.queued().decrement();
            task.changeStatus(DEQUEUED);

            consumer.accept(task);
            task.changeStatus(SCHEDULED);
        }
    }

    void executeTask(InterfaceTask<?> task) {
        try {
            task.execute();
        } finally {
            counters.running().decrement();
            counters.finished().increment();
            InterfaceThreadMonitor.finish(isProgressThread());
            task.changeStatus(FINISHED);
            unparkMonitor();
        }
    }

    private void parkMonitor() {
        // monitor thread parking itself
        if (maxActiveTasksExceeded()) {
            LockSupport.park();
        }

    }

    private void unparkMonitor() {
        // background thread unparking the monitor
        if (!maxActiveTasksExceeded()) {
            LockSupport.unpark(monitor);
        }
    }

    @Override
    public void disposeInner() {
        while(queue.peek() != null) {
            InterfaceTask<?> task = queue.remove();
            task.changeStatus(CANCELLED);
        }
        counters.queued().reset();
    }

    public Project getProject() {
        return getConnection().getProject();
    }
}
