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
import com.dbn.batch.BatchCounters;
import com.dbn.batch.BatchInput;
import com.dbn.batch.BatchMessenger;
import com.dbn.batch.BatchTask;
import com.dbn.common.message.MessageType;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;

import static com.dbn.nls.NlsResources.txt;

public abstract class BatchMessengerBase<
        T extends BatchTask,
        I extends BatchInput<T>,
        B extends Batch<I, T>> implements BatchMessenger<T, I, B> {

    protected String cleanExceptionMessage(B batch, String message) {
        DatabaseMessageParserInterface messageParserInterface = batch.getConnection().getMessageParserInterface();
        return messageParserInterface.convertToPresentable(message);
    }

    @Override
    public MessageType getProgressMessageType(B batch) {
        if (batch.isRunning() || batch.isPaused()) return MessageType.NEUTRAL;
        BatchCounters counters = batch.getCounters();

        if (counters.successItems() == counters.queuedItems()) return MessageType.SUCCESS;
        if (counters.failedItems() == counters.queuedItems()) return MessageType.ERROR;
        return MessageType.WARNING;
    }

    protected String getProgressText(B batch) {
        BatchCounters counters = batch.getCounters();
        int queuedCount = counters.queuedItems();
        int successCount = counters.successItems();
        int errorCount = counters.failedItems();

        String successSegment = txt("app.batch.text.TasksCompleted", successCount, queuedCount);
        String errorSegment = errorCount == 0 ? "" : txt("app.batch.text.TasksFailed", errorCount);

        if (batch.isCancelled() || batch.isFinished())  {
            int cancelCount = queuedCount - counters.processedItems();
            String cancelSegment = cancelCount == 0 ? "" : txt("app.batch.text.TasksCancelled", cancelCount);
            return txt("app.batch.text.Progress", successSegment, errorSegment, cancelSegment);
        } else {
            return txt("app.batch.text.Progress", successSegment, errorSegment, "");
        }
    }
}
