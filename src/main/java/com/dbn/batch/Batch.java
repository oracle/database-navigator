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

package com.dbn.batch;

import com.dbn.batch.event.BatchEventListener;
import com.dbn.batch.event.BatchEventType;
import com.dbn.common.message.MessageBundle;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;

public interface Batch<I extends BatchInput<T>, T extends BatchTask> {
    I getInput();

    Project getProject();

    ConnectionHandler getConnection();

    Object getContextObject();

    BatchProcessor<T, I, ? extends Batch<I, T>> getProcessor();

    BatchMessenger<T, I, ? extends Batch<I, T>> getMessenger();


    int getInitialTaskCount();

    int getCompletedTaskCount();

    Queue<T> getTasks();

    MessageBundle getMessages();

    BatchStatus getStatus();

    void init();
    void start();
    void pause();
    void resume();
    void cancel();

    boolean isRunning();
    boolean isPaused();
    boolean isFinished();
    boolean isCancelled();


    void notifyEvent(@NotNull BatchEventType type);

    void notifyEvent(@NotNull BatchEventType type, @Nullable BatchTask task);

    void addEventListener(BatchEventListener listener);
}
