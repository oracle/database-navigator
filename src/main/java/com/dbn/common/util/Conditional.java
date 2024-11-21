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


import com.dbn.common.routine.ParametricRunnable;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.guarded;

@UtilityClass
public class Conditional {

    public static void when(boolean condition, @Nullable Runnable runnable) {
        if (runnable == null) return;
        if (!condition) return;

        guarded(runnable, r -> r.run());
    }

    public static <T, E extends Throwable> void whenValid(@Nullable T object, ParametricRunnable<T, E> runnable) throws E{
        if (runnable == null) return;
        if (isNotValid(object)) return;

        guarded(object, runnable);
    }

    public static <T, E extends Throwable> void whenNotNull(@Nullable T object, ParametricRunnable<T, E> runnable) throws E{
        if (runnable == null) return;
        if (object == null) return;

        guarded(object, runnable);;
    }

    public static <E extends Throwable> void whenNotEmpty(@Nullable String string, ParametricRunnable<String, E> runnable) throws E{
        if (runnable == null) return;
        if (Strings.isEmptyOrSpaces(string)) return;

        guarded(string, runnable);
    }
}
