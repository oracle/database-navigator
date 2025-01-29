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

import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

@Slf4j
@UtilityClass
public final class Measured {

    @SneakyThrows
    public static void run(@NonNls String identifier, ThrowableRunnable<Throwable> runnable) {
        logStart(identifier);
        long start = System.currentTimeMillis();
        try {
            runnable.run();
        } finally {
            logEnd(identifier, start);
        }
    }

    @SneakyThrows
    public static <T> T call(@NonNls String identifier, ThrowableCallable<T, Throwable> callable) {
        logStart(identifier);
        long start = System.currentTimeMillis();
        try {
            return callable.call();
        } finally {
            logEnd(identifier, start);
        }
    }

    private static void logStart(String identifier) {
        log.info("[DBN] Started " + identifier);
    }

    private static void logEnd(String identifier, long start) {
        log.info("[DBN] Done " + identifier + " - " + (System.currentTimeMillis() - start) + "ms");
    }
}
