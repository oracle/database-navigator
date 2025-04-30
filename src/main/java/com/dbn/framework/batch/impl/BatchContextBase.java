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

package com.dbn.framework.batch.impl;

import com.dbn.common.Priority;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCollector;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TaggedMessage;
import com.dbn.common.message.ui.MessageBundleDialog;
import com.dbn.common.message.ui.MessageBundleDialogConfig;
import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.framework.batch.BatchContext;
import com.dbn.framework.batch.BatchElement;
import com.dbn.framework.batch.BatchInput;
import com.dbn.framework.batch.BatchProducer;
import com.dbn.framework.batch.BatchTask;
import com.dbn.framework.task.TaskQueue;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.outcome.OutcomeType.FAILURE;
import static com.dbn.common.outcome.OutcomeType.SUCCESS;

@Getter
public abstract class BatchContextBase<E extends BatchElement, I extends BatchInput<E>, T extends BatchTask> implements BatchContext<I, T>, MessageBundle {
    private final I input;
    private final List<T> tasks = new ArrayList<>();
    private final TaskQueue queue = new TaskQueue();
    private final BatchProducer messageProducer;

    @Delegate
    private final MessageBundle messageBundle = new MessageCollector();

    public BatchContextBase(I input) {
        this.input = input;
        this.queue.addOutcomeHandler(SUCCESS, createSuccessMessageCollector());
        this.queue.addOutcomeHandler(FAILURE, createFailureMessageCollector());
        this.queue.addOutcomeHandler(FAILURE, createFailedProcessInterrupter());

        this.messageProducer = createMessageProducer();
    }

    public void queueTask(Runnable runnable) {
        queueTask(null, runnable);
    }

    public void queueTask(@Nullable Object subject, Runnable runnable) {
        queue.push(subject, runnable);
    }

    public void executeQueuedTasks() {
        queue.execute();
    }

    public boolean isComplete() {
        return queue.isEmpty();
    }

    public int errorNotificationThreshold() {
        return 3;
    }

    protected abstract BatchProducer createMessageProducer();

    protected abstract T createBatchTask(E element);

    private OutcomeHandler createSuccessMessageCollector() {
        return new OutcomeHandler() {
            @Override
            public void handle(Outcome outcome) {
                MessageType messageType = MessageType.INFO;
                Object data = outcome.getData();
                String messageText = messageProducer.createSuccessMessage(data);

                Message message = data == null ?
                        new Message(messageType, messageText) :
                        new TaggedMessage<>(messageType, messageText, data);
                messageBundle.addMessage(message);
            }

            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
    }

    /**
     * Primary failure outcome handler for collecting error messages
     * @return an {@link OutcomeHandler}
     */
    private OutcomeHandler createFailureMessageCollector() {
        return new OutcomeHandler() {
            @Override
            public void handle(Outcome outcome) {
                MessageType messageType = MessageType.ERROR;
                Object data = outcome.getData();
                String messageText = messageProducer.createErrorMessage(data, outcome.getException());

                Message message = data == null ?
                        new Message(messageType, messageText) :
                        new TaggedMessage<>(messageType, messageText, data);
                messageBundle.addMessage(message);
            }

            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
    }

    /**
     * Secondary failure outcome handler to allow the user to interrupt the process if there are issues with one or more items.
     * @return an {@link OutcomeHandler}
     */
    private OutcomeHandler createFailedProcessInterrupter() {
        return new OutcomeHandler() {
            @Override
            public void handle(Outcome outcome) {

                // only prompt after more than 3 errors or if the outcome is terminal
                if (!isComplete() && countErrors() < errorNotificationThreshold()) return;

                int exitCode = Dialogs.prompt(() -> createMessageBundleDialog());

                if (exitCode == DialogWrapper.CANCEL_EXIT_CODE) {
                    ProgressMonitor.cancelProgress();
                }

            }

            @Override
            public Priority getPriority() {
                return Priority.MEDIUM; // allow the failure collection to happen first
            }
        };
    }

    protected void addTask(T task) {
        tasks.add(task);
    }

    @NotNull
    public final DatabaseContext getDatabaseContext() {
        return input.getDatabaseContext();
    }

    @Override
    public final Project getProject() {
        return input.getProject();
    }

    private @NotNull MessageBundleDialog createMessageBundleDialog() {
        MessageBundleDialogConfig config = MessageBundleDialogConfig
                .create(getProject(), "Processing Errors")
                .withContextObject(getContextObject())
                .withMainMessage(messageProducer.createHeaderMessage());

        return new MessageBundleDialog(config, this);
    }


    public boolean hasErrors() {
        return messageBundle.hasErrors();
    }
}
