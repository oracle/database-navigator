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

package com.dbn.assistant;

import com.dbn.common.compatibility.Workaround;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.common.util.Classes;

/**
 * Thread context classloader wrappers<br>
 * Motivation:
 * <p>
 * 1. The internal jackson initialization favors ide class loader,
 * causing it to initialize on old jackson libraries provided by intellij
 * (these are incompatible with the current version of langchain4j)
 * <p>
 * 2. The internal httpProvider initialization using ServiceLoader favors the thread context class loader
 * (jersey http client implementation fails to load unless the plugin class loader is used)
 */
public interface AssistantComponent {
    @Workaround
    default <T, E extends Throwable> T wrapped(ThrowableCallable<T, E> callable) throws E{
        return Classes.withClassLoader(getClass(), callable);
    }

    @Workaround
    default <E extends Throwable> void wrapped(ThrowableRunnable<E> runnable) throws E {
        Classes.withClassLoader(getClass(), () -> {
            runnable.run();
            return null;
        });
    }
}
