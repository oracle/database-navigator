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

import com.dbn.common.routine.ParametricCallable;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@UtilityClass
public class Classes {

    public static <P, R, E extends Throwable> R withClassLoader(P param, ParametricCallable<P, R, E> callable) throws E{
        Thread thread = Thread.currentThread();
        ClassLoader currentClassLoader = thread.getContextClassLoader();
        try {
            ClassLoader paramClassLoader = param.getClass().getClassLoader();
            thread.setContextClassLoader(paramClassLoader);
            return callable.call(param);
        } finally {
            thread.setContextClassLoader(currentClassLoader);
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
}
