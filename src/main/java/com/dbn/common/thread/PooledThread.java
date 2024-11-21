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

import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Slf4j
public class PooledThread extends Thread{
    private static final ThreadGroup GROUP = new ThreadGroup("DBN");
    private static final Map<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();
    private Future<?> currentTask;
    private String currentTaskId;

    public PooledThread(String name, Runnable runnable) {
        super(GROUP, runnable, indexedName(name));
    }

    private static String indexedName(String name) {
        AtomicInteger index = COUNTERS.computeIfAbsent(name, n -> new AtomicInteger(0));
        return name + " " + index.incrementAndGet();
    }

    @Nullable
    public static PooledThread current() {
        Thread thread = Thread.currentThread();
        if (thread instanceof PooledThread) return (PooledThread) thread;
        return null;
    }

    public static String enter(Future<?> future) {
        PooledThread pooledThread = current();
        if (pooledThread == null) return null;

        String taskId = UUIDs.compact();
        String currentTaskId = pooledThread.getCurrentTaskId();

        if (currentTaskId != null) log.error("Incomplete thread context");
        pooledThread.setCurrentTask(future);
        pooledThread.setCurrentTaskId(taskId);
        return taskId;
    }

    public static void exit(String taskId) {
        PooledThread pooledThread = current();
        if (pooledThread == null) return;

        String currentTaskId = pooledThread.getCurrentTaskId();

        if (!Objects.equals(currentTaskId, taskId)) log.error("Invalid thread context");
        pooledThread.setCurrentTask(null);
        pooledThread.setCurrentTaskId(null);
    }

    public void cancel() {
        if (currentTask != null) currentTask.cancel(true);
    }

}
