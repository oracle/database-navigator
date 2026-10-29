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

package com.dbn.driver.download;

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.ThreadProperty;
import com.dbn.common.thread.Threads;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/** Runs blocking driver-package metadata work on a shared, concurrency-limited pool. */
@UtilityClass
public final class DriverPackageTaskExecutor {
    private static final int MAX_CONCURRENT_TASKS = 8;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            MAX_CONCURRENT_TASKS,
            Threads.createThreadFactory("DBN - Driver Package Task", true));

    /**
     * Processes items concurrently and delivers each result to the caller thread as it completes.
     * The shared pool bounds Maven I/O across database types and dashboard instances.
     */
    public static <I, O> void map(
            Collection<I> items,
            Function<I, O> mapper,
            Consumer<O> completionHandler) {
        if (items.isEmpty()) return;

        ProgressIndicator indicator = ProgressMonitor.getProgressIndicator();
        ThreadInfo invoker = ThreadInfo.copy();
        CompletionService<O> completionService = new ExecutorCompletionService<>(EXECUTOR);
        List<Future<O>> futures = new ArrayList<>(items.size());
        boolean completed = false;
        try {
            for (I item : items) {
                checkCanceled(indicator);
                futures.add(completionService.submit(() -> ThreadMonitor.surround(
                        invoker,
                        ThreadProperty.PROGRESS,
                        () -> {
                            checkCanceled(indicator);
                            return mapper.apply(item);
                        })));
            }

            for (int remaining = futures.size(); remaining > 0; remaining--) {
                Future<O> future = completionService.poll(100, TimeUnit.MILLISECONDS);
                while (future == null) {
                    checkCanceled(indicator);
                    future = completionService.poll(100, TimeUnit.MILLISECONDS);
                }
                completionHandler.accept(future.get());
            }
            completed = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessCanceledException();
        } catch (ExecutionException e) {
            throw propagate(e.getCause());
        } finally {
            if (!completed) futures.forEach(future -> future.cancel(true));
        }
    }

    private static void checkCanceled(ProgressIndicator indicator) {
        if (indicator != null) indicator.checkCanceled();
        if (Thread.currentThread().isInterrupted()) throw new ProcessCanceledException();
    }

    private static RuntimeException propagate(Throwable cause) {
        if (cause instanceof ProcessCanceledException exception) return exception;
        if (cause instanceof RuntimeException exception) return exception;
        if (cause instanceof Error error) throw error;
        return new RuntimeException(cause);
    }
}
