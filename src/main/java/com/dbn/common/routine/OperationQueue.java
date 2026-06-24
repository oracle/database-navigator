/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.routine;

import java.util.ArrayList;
import java.util.List;

/**
 * Coalesces callbacks while a single asynchronous operation is in progress.
 * The first enqueued callback starts the operation; later callbacks wait for
 * the same operation result instead of triggering duplicate work or dialogs.
 */
public class OperationQueue {
    private List<Callback> callbacks;

    public synchronized boolean enqueue(Runnable callback, boolean requiresSuccess) {
        if (callbacks != null) {
            callbacks.add(new Callback(callback, requiresSuccess));
            return false;
        }

        callbacks = new ArrayList<>();
        callbacks.add(new Callback(callback, requiresSuccess));
        return true;
    }

    public void complete(boolean success) {
        List<Callback> callbacks = clear();
        if (callbacks == null) return;

        callbacks.stream()
                .filter(callback -> success || !callback.requiresSuccess())
                .map(Callback::runnable)
                .forEach(Runnable::run);
    }

    private synchronized List<Callback> clear() {
        List<Callback> callbacks = this.callbacks;
        this.callbacks = null;
        return callbacks;
    }

    private record Callback(
            Runnable runnable,
            boolean requiresSuccess) {}
}
