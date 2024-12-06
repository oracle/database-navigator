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

import com.dbn.common.exception.Exceptions;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.TimeUtil;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Timeout {
    private static final Object lock = new Object();

    @SneakyThrows
    public static <T> T call(String identifier, int seconds, T defaultValue, boolean daemon, ThrowableCallable<T, Throwable> callable) {
        long start = System.currentTimeMillis();
        try {
            Threads.delay(lock);
            seconds = Diagnostics.timeoutAdjustment(seconds);
            ThreadInfo invoker = ThreadInfo.copy();
            ExecutorService executorService = Threads.timeoutExecutor(daemon);

            AtomicReference<Future<T>> future = new AtomicReference<>();
            AtomicReference<Throwable> exception = new AtomicReference<>();

            future.set(executorService.submit(() -> {
                String taskId = PooledThread.enter(future.get());
                try {
                    return ThreadMonitor.surround(
                            invoker,
                            ThreadProperty.TIMEOUT,
                            callable);
                } catch (Throwable e) {
                    conditionallyLog(e);
                    exception.set(e);
                    return null;
                } finally {
                    PooledThread.exit(taskId);
                }
            }));

            T result = waitFor(future.get(), seconds, TimeUnit.SECONDS);
            if (exception.get() != null) {
                throw exception.get();
            }
            return result;
        } catch (CancellationException e) {
            conditionallyLog(e);
            throw new ProcessCanceledException();
        } catch (TimeoutException | InterruptedException | RejectedExecutionException e) {
            conditionallyLog(e);
            String message = Commons.nvl(e.getMessage(), simpleClassName(e));
            log.warn("{} - Operation timed out after {}s (timeout = {}s). Defaulting to {}. Cause: {}", identifier, TimeUtil.secondsSince(start), seconds, defaultValue, message);
        } catch (ExecutionException e) {
            conditionallyLog(e);
            log.warn("{} - Operation failed after {}s (timeout = {}s). Defaulting to {}", identifier, TimeUtil.secondsSince(start), seconds, defaultValue, Exceptions.causeOf(e));
            throw e.getCause();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw e;
        }
        return defaultValue;
    }

    @SneakyThrows
    public static void run(int seconds, boolean daemon, ThrowableRunnable<Throwable> runnable) {
        long start = System.currentTimeMillis();
        try {
            Threads.delay(lock);
            seconds = Diagnostics.timeoutAdjustment(seconds);
            ThreadInfo invoker = ThreadInfo.copy();
            ExecutorService executorService = Threads.timeoutExecutor(daemon);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            AtomicReference<Throwable> exception = new AtomicReference<>();

            future.set(executorService.submit(() -> {
                String taskId = PooledThread.enter(future.get());
                try {
                    ThreadMonitor.surround(
                            invoker,
                            ThreadProperty.TIMEOUT,
                            runnable);
                } catch (Throwable e) {
                    conditionallyLog(e);
                    exception.set(e);
                } finally {
                    PooledThread.exit(taskId);
                }
            }));
            waitFor(future.get(), seconds, TimeUnit.SECONDS);
            if (exception.get() != null) {
                throw exception.get();
            }
        } catch (CancellationException e) {
            conditionallyLog(e);
            throw new ProcessCanceledException();
        } catch (TimeoutException | InterruptedException | RejectedExecutionException e) {
            conditionallyLog(e);
            String message = Commons.nvl(e.getMessage(), simpleClassName(e));
            log.warn("Operation timed out after {}s (timeout = {}s). Cause: {}", TimeUtil.secondsSince(start), seconds, message);
        } catch (ExecutionException e) {
            conditionallyLog(e);
            log.warn("Operation failed after {}s (timeout = {}s)", TimeUtil.secondsSince(start), seconds, Exceptions.causeOf(e));
            throw e.getCause();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw e;
        }
    }

    public static <T> T waitFor(Future<T> future, long time, TimeUnit timeUnit) throws InterruptedException, TimeoutException, ExecutionException {
        try {
            Progress.cancelCallback(() -> future.cancel(true));
            return future.get(time, timeUnit);
        } catch (TimeoutException | InterruptedException e) {
            conditionallyLog(e);
            future.cancel(true);
            throw e;
        }
    }

}
