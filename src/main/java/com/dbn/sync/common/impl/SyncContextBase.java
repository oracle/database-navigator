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

package com.dbn.sync.common.impl;

import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCollector;
import com.dbn.common.message.MessageType;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.sync.common.SyncContext;
import com.dbn.sync.common.SyncInput;
import com.dbn.sync.common.SyncTask;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class SyncContextBase<I extends SyncInput, T extends SyncTask> implements SyncContext<I, T> {
    private final I input;
    private final List<T> tasks = new ArrayList<>();
    private final MessageCollector messages = new MessageBundle();

    public SyncContextBase(I input) {
        this.input = input;
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

    public final void handled(ThrowableRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            handle(e);
        }
    }

    @Nullable
    public final <R> R handled(ThrowableCallable<R, Exception> runnable) {
        try {
            return runnable.call();
        } catch (Throwable e) {
            handle(e);
            return null;
        }
    }

    private void handle(Throwable e) {
        messages.addMessage(new Message(MessageType.ERROR, e.getMessage()));
    }

    public boolean hasErrors() {
        return messages.hasErrors();
    }
}
