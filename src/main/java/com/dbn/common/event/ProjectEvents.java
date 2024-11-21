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

package com.dbn.common.event;

import com.dbn.common.project.Projects;
import com.dbn.common.routine.Consumer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.dispose.Failsafe.nd;


@UtilityClass
public final class ProjectEvents {

    public static <T> void subscribe(@NotNull Project project, @Nullable Disposable parentDisposable, Topic<T> topic, T handler) {
        guarded(() -> {
            if (isNotValid(project) || project.isDefault()) return;

            MessageBus messageBus = messageBus(project);
            MessageBusConnection connection = parentDisposable == null ?
                    messageBus.connect() :
                    messageBus.connect(nd(parentDisposable));

            connection.subscribe(topic, handler);
        });
    }

    public static <T> void subscribe(Topic<T> topic, T handler) {
        for (Project openProject : Projects.getOpenProjects()) {
            subscribe(openProject, null, topic, handler);
        }

        Application application = ApplicationManager.getApplication();
        application.invokeLater(() ->
                Projects.projectOpened(project ->
                    subscribe(project, null, topic, handler)));

    }

    public static <T> void notify(@Nullable Project project, Topic<T> topic, Consumer<T> consumer) {
        guarded(() -> {
            if (isNotValid(project) || project.isDefault()) return;
            T publisher = publisher(project, topic);
            consumer.accept(publisher);
        });
    }

    @NotNull
    private static <T> T publisher(@Nullable Project project, Topic<T> topic) {
        MessageBus messageBus = messageBus(project);
        return messageBus.syncPublisher(topic);
    }

    @NotNull
    private static MessageBus messageBus(@Nullable Project project) {
        return nd(project).getMessageBus();
    }
}
