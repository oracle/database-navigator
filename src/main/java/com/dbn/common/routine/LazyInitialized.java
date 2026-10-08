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

import com.dbn.common.thread.Background;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadMonitor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts one-time initialization when the first operation needs initialized state.
 * Initialization runs in the background, so callers do not wait for it to finish.
 * Operations submitted before it succeeds are deferred; later operations run immediately.
 *
 * <p>If initialization fails, deferred operations are not run and initialization is not retried.
 * A later call to {@link #whenInitialized(Runnable)} reports the stored failure.
 */
public abstract class LazyInitialized {
    private final AtomicReference<CompletableFuture<Void>> initialization = new AtomicReference<>();

    /**
     * Performs the one-time setup on a background thread. Implementations may do slow work
     * here and must return only when the state needed by deferred operations is ready.
     */
    protected abstract void initialize();

    /**
     * Runs an operation after successful initialization, starting initialization on first use.
     * This method returns without waiting while initialization is in progress.
     *
     * <p>If initialization has already completed, the operation runs on the calling thread.
     * A deferred operation has no EDT guarantee; it retains the caller's {@link ThreadInfo}.
     * Operations that update the UI must arrange their own dispatch.
     *
     * @param operation work that requires initialized state
     */
    public final void whenInitialized(@NotNull Runnable operation) {
        CompletableFuture<Void> current = initialization.get();
        boolean start = false;
        if (current == null) {
            CompletableFuture<Void> pending = new CompletableFuture<>();
            if (initialization.compareAndSet(null, pending)) {
                current = pending;
                start = true;
            } else {
                current = initialization.get();
            }
        }

        if (current.isDone()) {
            current.join();
            operation.run();
        } else {
            ThreadInfo invoker = ThreadInfo.copy();
            current.thenRun(() -> ThreadMonitor.surround(invoker, null, operation::run));
        }

        if (start) startInitialization(current);
    }

    private void startInitialization(CompletableFuture<Void> completion) {
        Background.run(() -> {
            try {
                if (ThreadMonitor.isDispatchThread()) {
                    throw new IllegalStateException("Initialization must run outside the EDT");
                }
                initialize();
                completion.complete(null);
            } catch (Throwable e) {
                completion.completeExceptionally(e);
                throw e;
            }
        });
    }
}
