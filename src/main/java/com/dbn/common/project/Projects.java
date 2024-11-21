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

import com.dbn.common.dispose.AlreadyDisposedException;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Unsafe;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.util.IncorrectOperationException;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.util.Conditional.when;

@UtilityClass
public final class Projects {

    public static final Project[] EMPTY_PROJECT_ARRAY = new Project[0];

    public static void closeProject(@NotNull Project project) {
        Dispatch.run(() -> {
            ProjectManager.getInstance().closeProject(project);
            WelcomeFrame.showIfNoProjectOpened();
        });
    }

    public static void projectOpened(Consumer<Project> consumer) {
        ApplicationEvents.subscribe(null, ProjectManager.TOPIC,
                new ProjectManagerListener() {
                    @Override
                    public void projectOpened(@NotNull Project project) {
                        guarded(() -> consumer.accept(project));
                    }
                });
    }

    public static void projectClosing(Consumer<Project> consumer) {
        ApplicationEvents.subscribe(null, ProjectManager.TOPIC,
                new ProjectManagerListener() {
                    @Override
                    public void projectClosing(@NotNull Project project) {
                        guarded(() -> consumer.accept(project));
                    }
                });

    }

    public static void projectClosed(Consumer<Project> runnable) {
        ApplicationEvents.subscribe(null, ProjectManager.TOPIC,
                new ProjectManagerListener() {
                    @Override
                    public void projectClosed(@NotNull Project project) {
                        guarded(() -> runnable.accept(project));
                    }
                });

    }

    public static @NotNull Project[] getOpenProjects() {
        return Unsafe.silent(EMPTY_PROJECT_ARRAY, () -> ProjectManager.getInstance().getOpenProjects());
    }

    public static Project getDefaultProject() {
        try {
            return ProjectManager.getInstance().getDefaultProject();
        } catch (IncorrectOperationException e) {
            throw AlreadyDisposedException.INSTANCE;
        }
    }


    public static Disposable closeAwareDisposable(Project project) {
        // void disposable - nothing to dispose, used in the disposal chains as parent-disposable
        Disposable disposable = () -> {};
        projectClosed(p -> when(p == project, () -> Disposer.dispose(disposable)));
        return disposable;
    }
}
