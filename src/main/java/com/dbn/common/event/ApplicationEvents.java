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

import com.dbn.common.routine.Consumer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.dispose.Failsafe.nd;

@UtilityClass
public final class ApplicationEvents {

    public static <T> void subscribe(@Nullable Disposable parentDisposable, Topic<T> topic, T handler) {
        guarded(() -> {
            MessageBus messageBus = messageBus();
            MessageBusConnection connection = parentDisposable == null ?
                    messageBus.connect() :
                    messageBus.connect(nd(parentDisposable));
            connection.subscribe(topic, handler);
        });
    }

    public static <T> void notify(Topic<T> topic, Consumer<T> consumer) {
        T publisher = publisher(topic);
        consumer.accept(publisher);
    }

    public static <T> T publisher(Topic<T> topic) {
        MessageBus messageBus = messageBus();
        return messageBus.syncPublisher(topic);
    }

    @NotNull
    private static MessageBus messageBus() {
        return ApplicationManager.getApplication().getMessageBus();
    }
}
