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

package com.dbn.batch.impl;

import com.dbn.batch.Batch;
import com.dbn.batch.BatchInput;
import com.dbn.batch.BatchMessenger;
import com.dbn.batch.BatchProcessor;
import com.dbn.batch.BatchStatus;
import com.dbn.batch.BatchTask;
import com.dbn.batch.event.BatchEvent;
import com.dbn.batch.event.BatchEventListener;
import com.dbn.batch.event.BatchEventType;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCollector;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.context.DatabaseContext;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;

import static com.dbn.batch.BatchStatus.CANCELLED;
import static com.dbn.batch.BatchStatus.FINISHED;
import static com.dbn.batch.BatchStatus.NEW;
import static com.dbn.batch.BatchStatus.PAUSED;
import static com.dbn.batch.BatchStatus.RUNNING;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public abstract class BatchBase<
        T extends BatchTask,
        I extends BatchInput<T>> implements Batch<I, T> {

    private final I input;
    private final Queue<T> tasks;
    private final int initialTaskCount;
    private final BatchProcessor<T, I, Batch<I, T>> processor;
    private final BatchMessenger<T, I, Batch<I, T>> messenger;

    private final MessageBundle messages = new MessageCollector();
    private final Listeners<BatchEventListener> listeners = Listeners.create();

    @Delegate
    private BatchStatus status = NEW;

    public BatchBase(I input) {
        this.input = input;
        this.messenger = cast(createMessenger());
        this.processor = cast(createProcessor());
        this.tasks = new LinkedList<>(input.getSelectedTasks());
        this.initialTaskCount = tasks.size();
        addEventListener(createProcessStatusListener());
    }

    @Override
    public int getCompletedTaskCount() {
        return initialTaskCount - tasks.size();
    }

    public void start() {
        processor.start(this);
    }

    @Override
    public void pause() {
        processor.pause(this);
    }

    @Override
    public void resume() {
        processor.resume(this);
    }

    @Override
    public void cancel() {
        processor.cancel(this);
    }

    protected abstract BatchMessenger<T, I, ? extends Batch<I, T>> createMessenger();
    protected abstract BatchProcessor<T, I, ? extends Batch<I, T>> createProcessor();


    private BatchEventListener createProcessStatusListener() {
        // update process status based on the process-level events
        return event -> {
            if (event.getTask() != null) return; // ignore task-level events

            BatchEventType type = event.getType();

            switch (type) {
                case STARTED:
                case RESUMED: status = RUNNING; break;
                case PAUSED: status = PAUSED; break;
                case FINISHED: status = FINISHED; break;
                case CANCELLED: status = CANCELLED; break;
                default:
            }
        };
    }

    @Override
    public void notifyEvent(@NotNull BatchEventType type) {
        notifyEvent(type, null);
    }

    @Override
    public void notifyEvent(@NotNull BatchEventType type, @Nullable BatchTask task) {
        BatchEvent event = new BatchEvent(type, this, task);
        listeners.notify(e -> e.eventOccurred(event));
    }

    @Override
    public void addEventListener(BatchEventListener listener) {
        listeners.add(listener);
    }

    @NotNull
    public final DatabaseContext getDatabaseContext() {
        return input.getDatabaseContext();
    }

    public final ConnectionHandler getConnection() {
        return getDatabaseContext().ensureConnection();
    }

    public final ConnectionId getConnectionId() {
        return getConnection().getConnectionId();
    }

    public final String getConnectionName() {
        return getConnection().getName();
    }

    @Override
    public final Project getProject() {
        return input.getProject();
    }
}
