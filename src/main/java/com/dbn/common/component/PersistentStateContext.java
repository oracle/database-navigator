/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.component;

import com.dbn.common.project.ProjectContext;
import com.dbn.common.state.StateEncryptionCache;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

public class PersistentStateContext {
    private static final ThreadLocal<StateEncryptionCache> encryptionCache = new ThreadLocal<>();

    @Nullable
    public static StateEncryptionCache getEncryptionCache() {
        return encryptionCache.get();
    }

    public static void surround(@Nullable Project project, @Nullable StateEncryptionCache cache, Runnable runnable) {
        ProjectContext.surround(project, () -> surround(cache, runnable));
    }

    public static <T> T surround(@Nullable Project project, @Nullable StateEncryptionCache cache, Callable<T> callable) {
        return ProjectContext.surround(project, () -> surround(cache, callable));
    }

    private static void surround(@Nullable StateEncryptionCache cache, Runnable runnable) {
        if (cache == null) {
            runnable.run();
            return;
        }

        StateEncryptionCache initial = encryptionCache.get();
        try {
            encryptionCache.set(cache);
            runnable.run();
        } finally {
            if (initial == null) {
                encryptionCache.remove();
            } else {
                encryptionCache.set(initial);
            }
        }
    }

    private static <T> T surround(@Nullable StateEncryptionCache cache, Callable<T> callable) throws Exception {
        if (cache == null) return callable.call();

        StateEncryptionCache initial = encryptionCache.get();
        try {
            encryptionCache.set(cache);
            return callable.call();
        } finally {
            if (initial == null) {
                encryptionCache.remove();
            } else {
                encryptionCache.set(initial);
            }
        }
    }
}
