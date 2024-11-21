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

package com.dbn.common.routine;

import com.dbn.common.thread.Background;
import com.intellij.util.containers.ContainerUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dbn.common.util.Unsafe.warned;

public final class AsyncTaskExecutor {
    private static final Predicate<Future> ACTIVE = f -> !f.isDone();
    private static final Consumer<Future> CANCEL = f -> warned(() -> f.cancel(true));
    private static final Function<Future, Callable<Object>> CONSUME = f -> warned(null, () -> () -> f.get());

    private final ExecutorService executor;
    private final Set<Future> tasks = ContainerUtil.newConcurrentSet();
    private @Getter boolean finished;

    private final long timeout;
    private final TimeUnit timeUnit;

    public AsyncTaskExecutor(ExecutorService executor, long timeout, TimeUnit timeUnit) {
        this.executor = executor;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public void submit(Runnable runnable) {
        tasks.add(executor.submit(() -> warned(() -> runnable.run())));
    }

    public void complete() {
        try {
            Set<Future> tasks = getAndDiscardTasks();
            List<Callable<Object>> consumers = tasks
                    .stream()
                    .filter(ACTIVE)
                    .map(CONSUME)
                    .filter(c -> c != null)
                    .collect(Collectors.toList());

            warned(() -> executor.invokeAll(consumers, timeout, timeUnit));
            cancel(tasks);
        } finally {
            finished = true;
        }
    }

    public void cancel() {
        try {
            Set<Future> tasks = getAndDiscardTasks();
            cancel(tasks);
        } finally {
            finished = true;
        }
    }

    private static void cancel(Collection<Future> tasks) {
        Background.run(null, () -> tasks
                .stream()
                .filter(ACTIVE)
                .forEach(CANCEL));
    }

    @NotNull
    private Set<Future> getAndDiscardTasks() {
        Set<Future> tasks = new HashSet<>(this.tasks);
        this.tasks.clear();
        return tasks;
    }
}
