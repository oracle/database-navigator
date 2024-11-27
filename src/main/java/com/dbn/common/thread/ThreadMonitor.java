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

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class ThreadMonitor {
    private static final Map<ThreadProperty, AtomicInteger> PROCESS_COUNTERS = new ConcurrentHashMap<>();

    @Nullable
    public static Project getProject() {
        return null; //ThreadInfo.current().getProject();
    }

    public static <E extends Throwable> void surround(
            @Nullable ThreadProperty property,
            ThrowableRunnable<E> runnable) throws E {

        surround(property, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T, E extends Throwable> T surround(
            @Nullable ThreadProperty property,
            ThrowableCallable<T, E> callable) throws E {
        ThreadInfo threadInfo = ThreadInfo.current();

        try {
            if (property != null) threadInfo.set(property, true);
            return callable.call();
        } finally {
            if (property != null) threadInfo.set(property, false);
        }
    }


    public static <E extends Throwable> void surround(
            @Nullable ThreadInfo invoker,
            @Nullable ThreadProperty property,
            ThrowableRunnable<E> runnable) throws E {

        surround(invoker, property, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T, E extends Throwable> T surround(
            @Nullable ThreadInfo invoker,
            @Nullable ThreadProperty property,
            ThrowableCallable<T, E> callable) throws E {

        ThreadInfo threadInfo = ThreadInfo.current();

        boolean originalProperty = false;
        AtomicInteger processCounter = null;

        if (property != null) {
            originalProperty = threadInfo.is(property);
            processCounter = getProcessCounter(property);
        }

        try {
            threadInfo.merge(invoker);
            if (property != null) {
                processCounter.incrementAndGet();
                threadInfo.set(property, true);
            }

            return callable.call();

        } finally {
            if (property != null)  {
                processCounter.decrementAndGet();
                threadInfo.set(property, originalProperty);
            }
            threadInfo.unmerge(invoker);
        }
    }

    public static boolean isTimeoutProcess() {
        return ThreadInfo.current().is(ThreadProperty.TIMEOUT);
    }

    public static boolean isBackgroundProcess() {
        return ThreadInfo.current().is(ThreadProperty.BACKGROUND);
    }

    public static boolean isProgressProcess() {
        return ThreadInfo.current().is(ThreadProperty.PROGRESS) || ProgressMonitor.isProgress();
    }

    public static boolean isModalProcess() {
        return ThreadInfo.current().is(ThreadProperty.MODAL) || ProgressMonitor.isProgressModal();
    }

    public static boolean isDisposerProcess() {
        return ThreadInfo.current().is(ThreadProperty.DISPOSER);
    }

    public static boolean isDispatchThread() {
        Application application = ApplicationManager.getApplication();
        return application != null && application.isDispatchThread();
    }

    public static boolean isReadActionThread() {
        Application application = ApplicationManager.getApplication();
        return application != null && application.isReadAccessAllowed();
    }

    public static boolean isWriteActionThread() {
        Application application = ApplicationManager.getApplication();
        return application != null && application.isWriteAccessAllowed();
    }

    public static boolean isTimeSensitiveThread() {
        if (isDispatchThread()) return true;
        if (isWriteActionThread()) return true;
        if (isReadActionThread()) return true;

        if (isBackgroundProcess()) return false;
        if (isTimeoutProcess()) return false;
        if (isProgressProcess()) return isModalProcess();

        return true;
    }

    public static int getProcessCount(ThreadProperty property) {
        return getProcessCounter(property).intValue();
    }

    private static AtomicInteger getProcessCounter(ThreadProperty property) {
        return PROCESS_COUNTERS.computeIfAbsent(property, p -> new AtomicInteger(0));
    }

    public static <E extends Throwable> void wrap(@NotNull ThreadProperty threadProperty, ThrowableRunnable<E> runnable) throws E {
        ThreadInfo threadInfo = ThreadInfo.current();
        boolean original = threadInfo.is(threadProperty);
        try {
            threadInfo.set(threadProperty, true);
            runnable.run();
        }
        finally {
            threadInfo.set(threadProperty, original);
        }
    }

    public static <R, E extends Throwable> R wrap(@NotNull ThreadProperty threadProperty, ThrowableCallable<R, E> callable) throws E {
        ThreadInfo threadInfo = ThreadInfo.current();
        boolean original = threadInfo.is(threadProperty);
        try {
            threadInfo.set(threadProperty, true);
            return callable.call();
        }
        finally {
            threadInfo.set(threadProperty, original);
        }
    }
}
