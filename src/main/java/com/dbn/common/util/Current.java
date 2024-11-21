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

package com.dbn.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.Future;

@Slf4j
@UtilityClass
public class Current {
    private static final ThreadLocal<Future> currentTask = new ThreadLocal<>();
    private static final ThreadLocal<String> currentTaskId = new ThreadLocal<>();

    public static String start() {
        return start(null);
    }

    public static String start(Future<?> task) {
        if (Current.currentTaskId.get() != null) {
            log.error("Incomplete thread context");
        }

        String taskId = UUIDs.compact();
        currentTaskId.set(taskId);
        currentTask.set(task);
        return taskId;
    }

    public static void end(String taskId) {
        if (!Objects.equals(currentTaskId.get(), taskId)) {
            log.error("Invalid thread context");
        }
        currentTaskId.remove();
        currentTask.remove();
    }

    public static String currentTaskId() {
        return currentTaskId.get();
    }

    public static <T> Future<T> currentTask() {
        return Unsafe.cast(currentTask.get());
    }

    public void cancel() {
        Future future = currentTask.get();
        if (future != null) future.cancel(true);
    }

}
