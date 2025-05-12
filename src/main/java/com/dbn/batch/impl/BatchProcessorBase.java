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
import com.dbn.batch.BatchProcessor;
import com.dbn.batch.BatchTask;
import com.dbn.common.thread.Background;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import java.util.Queue;

import static com.dbn.batch.event.BatchEventType.CANCELLED;
import static com.dbn.batch.event.BatchEventType.FINISHED;
import static com.dbn.batch.event.BatchEventType.PAUSED;
import static com.dbn.batch.event.BatchEventType.RESUMED;
import static com.dbn.batch.event.BatchEventType.STARTED;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
public abstract class BatchProcessorBase<
        T extends BatchTask,
        I extends BatchInput<T>,
        B extends Batch<I, T>>
                implements BatchProcessor<T, I, B> {

    private final String identifier;

    public BatchProcessorBase(@NonNls String identifier) {
        this.identifier = identifier;
    }

    @Override
    public final void start(B batch) {
        prepareBatch(batch);

        batch.notifyEvent(STARTED);
        Background.run(() -> processBatch(batch));
    }

    @Override
    public final void pause(B batch) {
        if (batch.isPaused()) return;
        if (batch.isFinished()) return;
        if (batch.isCancelled()) return;

        batch.notifyEvent(PAUSED);
    }

    public final void resume(B batch) {
        if (batch.isRunning()) return;
        if (batch.isFinished()) return;
        if (batch.isCancelled()) return;

        batch.notifyEvent(RESUMED);
        Background.run(() -> processBatch(batch));
    }

    @Override
    public void cancel(B batch) {
        if (batch.isFinished()) return;
        if (batch.isCancelled()) return;

        batch.notifyEvent(CANCELLED);
    }

    protected abstract void processTask(B batch, T task);

    @Deprecated // TODO execute before prompting the batch monitor
    protected void prepareBatch(B batch) {
        // no preparations needed by default
    }

    private void processBatch(B batch) {
        if (batch.isFinished()) return;
        if (batch.isCancelled()) return;

        Queue<T> tasks = batch.getTasks();
        while (!tasks.isEmpty()) {
            if (isInterrupted(batch)) return;

            T task = tasks.poll();
            if (task == null) return;

            try {
                batch.notifyEvent(STARTED, task);
                processTask(batch, task);

            } catch (Exception e) {
                conditionallyLog(e);
                task.setException(e);

            } finally {
                batch.notifyEvent(FINISHED, task);
            }

            if (isInterrupted(batch)) return;
        }

        // if reaching this point without being paused or canceled, it's safe to assume the process is finished
        batch.notifyEvent(FINISHED);
    }

    private boolean isInterrupted(B batch) {
        return batch.isPaused() || batch.isCancelled() || batch.isFinished();
    }
}
