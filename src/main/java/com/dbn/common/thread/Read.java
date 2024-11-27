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

import com.dbn.common.routine.ParametricCallable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.ThrowableComputable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class Read {

    public static <P, T, E extends Throwable> T call(P param, ParametricCallable<P, T, E> supplier) throws E {
        return getApplication().runReadAction((ThrowableComputable<T, E>) () -> supplier.call(param));
    }


    public static <T, E extends Throwable> T call(ThrowableComputable<T, E> supplier) throws E {
        return getApplication().runReadAction(supplier);
    }

    public static void run(Runnable runnable) {
        Application application = getApplication();
        application.runReadAction(runnable);
    }

    private static Application getApplication() {
        return ApplicationManager.getApplication();
    }
}
