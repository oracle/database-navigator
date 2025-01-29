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

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Synchronized;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.diagnostics.Diagnostics;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.dbn.common.util.Unsafe.silent;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Traces {

    private static final Set<String> SKIPPED_CALL_STACK_CLASSES = new HashSet<>(Arrays.asList(
            Traces.class.getName(),
            ThreadInfo.class.getName(),
            ThreadMonitor.class.getName(),
            Synchronized.class.getName(),
            Background.class.getName(),
            Progress.class.getName(),
            Failsafe.class.getName()));

    private static final Map<String, Class> classCache = new ConcurrentHashMap<>();

    private Class getClass(String className) {
        return classCache.computeIfAbsent(className, n -> silent(NoMatchSurrogate.class, () -> Class.forName(n)));
    }

    /**
     * Determines if a method is called through a class that matches the given predicate within a specified maximum stack depth.
     *
     * @param matcher a predicate that evaluates whether a given class matches certain conditions
     * @param maxDepth the maximum depth of the stack trace to search for a matching class
     * @return true if a method call originates through a class that satisfies the given matcher predicate within the given stack depth, false otherwise
     */
    public static boolean isCalledThroughClass(Predicate<Class> matcher, int maxDepth) {
        return isCalledThrough(n -> matcher.test(getClass(n)), maxDepth);
    }

    /**
     * Determines if the current method call is made through the specified class
     * within a given stack depth limit.
     *
     * @param clazz the class to check in the call stack
     * @param maxDepth the maximum depth to search in the call stack
     * @return true if the method is called through the specified class within the given depth, false otherwise
     */
    public static boolean isCalledThroughClass(Class clazz, int maxDepth) {
        return isCalledThrough(n -> Objects.equals(clazz.getName(), n), maxDepth);
    }

    /**
     * Checks if the current thread's call stack contains a class name that matches the specified predicate
     * within the specified maximum depth of the call stack.
     *
     * @param matcher a {@link Predicate} used to match class names in the call stack
     * @param maxDepth the maximum depth of the call stack to scan
     * @return true if a class name matching the predicate is found in the call stack within the specified depth, false otherwise
     */
    public static boolean isCalledThrough(Predicate<String> matcher, int maxDepth) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        try {
            int scanDepth = Math.min(callStack.length, maxDepth);
            for (int i = 3; i < scanDepth; i++) {
                StackTraceElement stackTraceElement = callStack[i];
                String className = stackTraceElement.getClassName();
                if (matcher.test(className)) {
                    return true;
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
        }
        return false;
    }

    public static boolean isCalledThrough(Class clazz, String methodName) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        try {
            for (int i = 3; i < callStack.length; i++) {
                StackTraceElement stackTraceElement = callStack[i];
                String className = stackTraceElement.getClassName();
                if (Objects.equals(clazz.getName(), className) /*|| clazz.isAssignableFrom(Class.forName(className))*/) {
                    String methName = stackTraceElement.getMethodName();
                    if (Objects.equals(methodName, methName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
        return false;
    }

    public static StackTraceElement[] diagnosticsCallStack() {
        if (!Diagnostics.isDeveloperMode()) return null;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Arrays
                .stream(stackTrace)
                .filter(st -> !SKIPPED_CALL_STACK_CLASSES.contains(st.getClassName()))
                .toArray(StackTraceElement[]::new);
    }

    private static class NoMatchSurrogate {}
}
