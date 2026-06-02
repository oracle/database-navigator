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
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Failsafe.nd;

@UtilityClass
public class Components {

    @NotNull
    public static <T extends ProjectComponent> T projectService(@NotNull Project project, @NotNull Class<T> serviceClass) {
        return nd(optionalProjectService(project, serviceClass));
    }

    @NotNull
    public static <T extends ApplicationComponent> T applicationService(@NotNull Class<T> serviceClass) {
        return nd(optionalApplicationService(serviceClass));
    }

    @Nullable
    public static <T extends ProjectComponent> T optionalProjectService(@NotNull Project project, @NotNull Class<T> serviceClass) {
        return isEagerService(serviceClass) ?
                nd(project).getComponent(serviceClass) :
                nd(project).getService(serviceClass);    }

    @Nullable
    public static <T extends ApplicationComponent> T optionalApplicationService(@NotNull Class<T> serviceClass) {
        Application application = ApplicationManager.getApplication();
        return isEagerService(serviceClass) ?
                application.getComponent(serviceClass) :
                application.getService(serviceClass);
    }

    private static <T extends Service> boolean isEagerService(@NotNull Class<T> interfaceClass) {
        return EagerService.class.isAssignableFrom(interfaceClass);
    }
}
