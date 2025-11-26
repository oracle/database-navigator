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
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@UtilityClass
public class Classes {
    public static <R, E extends Throwable> R withClassLoader(Object source, ThrowableCallable<R, E> callable) throws E {
        return withClassLoader(source.getClass(), callable);
    }

    public static <R, E extends Throwable> R withClassLoader(Class<?> source, ThrowableCallable<R, E> callable) throws E {
        return withClassLoader(source.getClassLoader(), callable);
    }

    public static <R, E extends Throwable> R withClassLoader(ClassLoader classLoader, ThrowableCallable<R, E> callable) throws E{
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(classLoader);
            return callable.call();
        } finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    @Nullable
    public static <T> Class<T> classForName(String name) {
        try {
            return Unsafe.cast(Class.forName(name));
        } catch (Throwable e) {
            conditionallyLog(e);
            return null;
        }
    }

    @NonNls
    public static String className(@Nullable Object object) {
        return object == null ? "null" : className(object.getClass());
    }

    @NonNls
    public static String className(@Nullable Class<?> clazz) {
        return clazz == null ? "null" : clazz.getName();
    }

    @NonNls
    public static String simpleClassName(@Nullable Object object) {
        return object == null ? "null" : simpleClassName(object.getClass());
    }

    @NonNls
    public static String simpleClassName(@Nullable Class<?> clazz) {
        return clazz == null ? "null" : clazz.getSimpleName();
    }
}
