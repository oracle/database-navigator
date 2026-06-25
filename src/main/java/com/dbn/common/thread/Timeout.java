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

import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.TimeUtil;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.exception.Exceptions.timeoutException;
import static com.dbn.common.exception.Exceptions.unwrap;
import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
/**
 * Lightweight timeout utilities for short guarded operations.
 * <p>
 * The utility runs work on a DBN timeout executor, waits for a bounded interval, and attempts
 * cooperative cancellation with {@link Future#cancel(boolean)} when the caller times out or is
 * interrupted. If an IDE progress indicator is bound to the current thread, progress cancellation
 * is also wired to the submitted task.
 * <p>
 * This is not a hard-stop mechanism for arbitrary long-running work. Callers that own blocking
 * resources such as JDBC statements, connections, sockets, or external processes should still use
 * their domain-specific timeout/cancel/close mechanisms.
 */
public final class Timeout {
    private static final Object lock = new Object();

    @SneakyThrows
    public static void run(int seconds, boolean daemon, ThrowableRunnable<Throwable> runnable) {
        call("Operation", seconds, null, daemon, () -> {
            runnable.run();
            return null;
        });
    }

    @SneakyThrows
    public static <T> T call(@NonNls String identifier, int seconds, T defaultValue, boolean daemon, ThrowableCallable<T, Throwable> callable) {
        long start = System.currentTimeMillis();
        TimeoutTask<T> task = null;
        try {
            Threads.delay(lock);
            seconds = Diagnostics.timeoutAdjustment(seconds);
            ThreadInfo invoker = ThreadInfo.copy();
            ExecutorService executorService = Threads.timeoutExecutor(daemon);

            task = new TimeoutTask<>(
                    () -> ThreadMonitor.surround(
                            invoker,
                            ThreadProperty.TIMEOUT,
                            callable));
            executorService.execute(task);

            T result = task.get(seconds, TimeUnit.SECONDS);
            task.propagateException();
            return result;

        } catch (CancellationException e) {
            // the submitted task was cancelled while waiting for completion.
            conditionallyLog(e);
            throw new ProcessCanceledException();

        } catch (InterruptedException e) {
            // the caller thread was interrupted while waiting for completion.
            conditionallyLog(e);
            cancelFuture(task);
            Thread.currentThread().interrupt();
            throw new ProcessCanceledException();

        } catch (TimeoutException | RejectedExecutionException e) {
            // the task exceeded the timeout or could not be submitted.
            conditionallyLog(e);
            cancelFuture(task);
            String message = Commons.nvl(e.getMessage(), simpleClassName(e));
            log.warn("{} - Operation timed out after {}s (timeout = {}s). Defaulting to {}. Cause: {}", identifier, TimeUtil.secondsSince(start), seconds, defaultValue, message);

        } catch (ExecutionException e) {
            // FutureTask wrapped a failure raised by task execution.
            conditionallyLog(e);
            Throwable cause = unwrap(e);
            log.warn("{} - Operation failed after {}s (timeout = {}s). Defaulting to {}", identifier, TimeUtil.secondsSince(start), seconds, defaultValue, cause);
            throw cause;

        } catch (Throwable e) {
            // any remaining failure propagates without timeout fallback.
            conditionallyLog(e);
            throw unwrap(e);
        }
        return defaultValue;
    }

    private static void cancelFuture(Future<?> future) {
        if (future != null) future.cancel(true);
    }

    public static <T> T waitFor(Future<T> future, long time, TimeUnit timeUnit) throws InterruptedException, TimeoutException, ExecutionException {
        try {
            Progress.cancelCallback(() -> future.cancel(true));
            return future.get(time, timeUnit);
        } catch (InterruptedException e) {
            conditionallyLog(e);
            future.cancel(true);
            throw e;
        } catch (TimeoutException e) {
            conditionallyLog(e);
            future.cancel(true);

            if (e.getMessage() == null) {
                throw timeoutException(time, timeUnit);
            }

            throw e;
        }
    }

    private static final class TimeoutTask<T> extends FutureTask<T> {
        private final AtomicReference<Throwable> exception;

        TimeoutTask(ThrowableCallable<T, Throwable> callable) {
            this(callable, new AtomicReference<>());
        }

        private TimeoutTask(ThrowableCallable<T, Throwable> callable, AtomicReference<Throwable> exception) {
            super(() -> {
                try {
                    return callable.call();
                } catch (Throwable e) {
                    conditionallyLog(e);
                    exception.set(e);
                    return null;
                }
            });
            this.exception = exception;
        }

        @Override
        public void run() {
            String taskId = PooledThread.enter(this);
            try {
                super.run();
            } finally {
                PooledThread.exit(taskId);
            }
        }

        @Override
        public T get(long timeout, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
            Progress.cancelCallback(() -> cancelFuture(this));
            return super.get(timeout, timeUnit);
        }

        @SneakyThrows
        private void propagateException() {
            Throwable throwable = exception.get();
            if (throwable != null) throw throwable;
        }
    }

}
