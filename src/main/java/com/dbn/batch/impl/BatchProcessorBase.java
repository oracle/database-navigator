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
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import java.util.Queue;

import static com.dbn.batch.event.BatchEventType.CANCELLED;
import static com.dbn.batch.event.BatchEventType.FINISHED;
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
    public final void process(B batch) {
        prepareBatch(batch);

        if (!batch.isCancelled()) {
            executeBatch(batch);
        }
    }

    protected void prepareBatch(B batch) {
        // no preparations needed by default
    }

    private void executeBatch(B batch) {
        Queue<T> tasks = batch.getTasks();
        batch.notifyEvent(STARTED);

        if (tasks.isEmpty()) {
            batch.notifyEvent(FINISHED);
            return;
        }

        while (!tasks.isEmpty()) {
            T task = tasks.poll();
            batch.notifyEvent(STARTED, task);
            try {
                processTask(batch, task);
                batch.notifyEvent(FINISHED, task);

            } catch (ProcessCanceledException e) {
                conditionallyLog(e);
                batch.notifyEvent(CANCELLED, task);

            } catch (Exception e) {
                conditionallyLog(e);
                task.setException(e);
                batch.notifyEvent(FINISHED, task);
            }

            if (batch.isInterrupted()) return;
        }

        // if reaching this point without being paused or canceled, it's safe to assume the process is finished
        batch.notifyEvent(FINISHED);
    }
}
