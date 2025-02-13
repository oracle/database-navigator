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

package com.dbn.common.project;

import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

public class ProjectContext {
    private static final ThreadLocal<ProjectRef> local = new ThreadLocal<>();

    public static void surround(Project project, Runnable runnable) {
        ProjectRef initial = local.get();
        try {
            local.set(ProjectRef.of(project));
            runnable.run();
        } finally {
            if (initial == null) {
                local.remove();
            } else {
                local.set(initial);
            }
        }
    }

    @SneakyThrows
    public static <T> T surround(Project project, Callable<T> callable) {
        // restore initial in case of nested invocations
        ProjectRef initial = local.get();
        try {
            local.set(ProjectRef.of(project));
            return callable.call();
        } finally {
            if (initial == null) {
                local.remove();
            } else {
                local.set(initial);
            }
        }
    }

    @Nullable
    public static Project getProject() {
        return ProjectRef.get(local.get());
    }
}
