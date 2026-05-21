/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.routine.ParametricCallable;
import com.dbn.common.routine.ParametricRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Unsafe.cast;

public class Recursion {
    private static final ThreadLocal<Map<String, Set<?>>> TRACES = new ThreadLocal<>();

    public static <T, S, E extends Throwable> T computeGuarded(String taskName, T defaultValue, S subject, ParametricCallable<S, T, E> callable) throws E{
        Set<S> traces = ensureTraces(taskName);
        if (traces.contains(subject)) return defaultValue;

        traces.add(subject);
        try {
            return callable.call(subject);
        } finally {
            traces.remove(subject);
        }
    }

    public static <S, E extends Throwable> void executeGuarded(String taskName, S subject, ParametricRunnable<S, E> runnable) throws E{
        Set<S> traces = ensureTraces(taskName);
        if (traces.contains(subject)) return;

        traces.add(subject);
        try {
            runnable.run(subject);
        } finally {
            traces.remove(subject);
        }
    }



    private static <S> Set<S> ensureTraces(String taskName) {
        Map<String, Set<?>> registry = Recursion.TRACES.get();
        if (registry == null) {
            registry = new HashMap<>();
            Recursion.TRACES.set(registry);
        }
        return cast(registry.computeIfAbsent(taskName, t -> new HashSet<>()));
    }
}
