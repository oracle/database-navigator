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

package com.dbn.common.component;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.nd;

public class Components {
    private Components() {}


    @NotNull
    public static <T extends ProjectComponent> T projectService(@NotNull Project project, @NotNull Class<T> interfaceClass) {
        return isEagerService(interfaceClass) ?
                nd(project).getComponent(interfaceClass) :
                nd(project).getService(interfaceClass);
    }

    @NotNull
    public static <T extends ApplicationComponent> T applicationService(@NotNull Class<T> interfaceClass) {
        Application application = ApplicationManager.getApplication();
        return isEagerService(interfaceClass) ?
            application.getComponent(interfaceClass) :
            application.getService(interfaceClass);
    }

    private static <T extends Service> boolean isEagerService(@NotNull Class<T> interfaceClass) {
        return EagerService.class.isAssignableFrom(interfaceClass);
    }
}
