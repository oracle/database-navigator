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

package com.dbn.common.thread;

import com.dbn.common.routine.ThrowableRunnable;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.thread.ThreadProperty.BACKGROUND;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Background {
    private static final Object lock = new Object();


    public static void run(ThrowableRunnable<Throwable> runnable) {
        try {
            Threads.delay(lock);
            ThreadInfo threadInfo = ThreadInfo.copy();
            ExecutorService executorService = Threads.backgroundExecutor();
            AtomicReference<Future<?>> future = new AtomicReference<>();

            future.set(executorService.submit(() -> {
                String taskId = PooledThread.enter(future.get());
                try {
                    ThreadMonitor.surround(
                            threadInfo,
                            BACKGROUND,
                            runnable);
                } catch (ProcessCanceledException | UnsupportedOperationException | InterruptedException e) {
                    conditionallyLog(e);
                } catch (SQLException e) {
                    log.warn("Error executing background task", e);
                } catch (Throwable e) {
                    log.error("Error executing background task", e);
                } finally {
                    PooledThread.exit(taskId);
                }
            }));
        } catch (RejectedExecutionException e) {
            conditionallyLog(e);
            log.warn("Background execution rejected: {}", e.getMessage());
        }
    }

    public static void run(AtomicReference<PooledThread> handle, ThrowableRunnable<Throwable> runnable) {
        try {
            Threads.delay(lock);
            PooledThread current = handle.get();
            if (current != null) current.cancel();

            ThreadInfo threadInfo = ThreadInfo.copy();
            ExecutorService executorService = Threads.backgroundExecutor();

            AtomicReference<Future<?>> future = new AtomicReference<>();

            future.set(executorService.submit(() -> {
                String taskId = PooledThread.enter(future.get());
                try {
                    try {
                        handle.set(PooledThread.current());
                        ThreadMonitor.surround(
                                threadInfo,
                                BACKGROUND,
                                runnable);
                    } finally {
                        handle.set(null);
                    }
                } catch (ProcessCanceledException | UnsupportedOperationException | InterruptedException e) {
                    conditionallyLog(e);
                } catch (SQLException e) {
                    log.warn("Error executing background task", e);
                } catch (Throwable e) {
                    log.error("Error executing background task", e);
                } finally {
                    PooledThread.exit(taskId);
                }
            }));
        } catch (RejectedExecutionException e) {
            conditionallyLog(e);
            log.warn("Background execution rejected: {}", e.getMessage());
        }
    }

}
