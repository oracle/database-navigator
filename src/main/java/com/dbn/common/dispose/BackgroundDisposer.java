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

package com.dbn.common.dispose;

import com.dbn.common.component.ApplicationMonitor;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.ThreadProperty;
import com.dbn.diagnostics.Diagnostics;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.thread.ThreadMonitor.isDisposerProcess;

@Slf4j
@Getter
@Setter
public final class BackgroundDisposer {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean running;

    private static final BackgroundDisposer INSTANCE = new BackgroundDisposer();

    private BackgroundDisposer() {}

    public static void queue(Runnable runnable) {
        if (isDisposerProcess() || Diagnostics.isBackgroundDisposerDisabled()) {
            runnable.run();
        } else {
            INSTANCE.push(runnable);
        }

    }

    private boolean isCancelled() {
        return ApplicationMonitor.isAppExiting();
    }

    private void push(Runnable runnable) {
        if (isCancelled()) return;
        queue.add(runnable);
        
        if (running || isCancelled()) return;

        synchronized (this) {
            if (running || isCancelled()) return;
            running = true;
            start();
        }
    }

    private void start() {
        Background.run(() -> {
            try {
                ThreadMonitor.wrap(ThreadProperty.DISPOSER, () -> dispose());
            } finally {
                running = false;
            }
        });
    }

    private void dispose() throws InterruptedException {
        while (!isCancelled()) {
            Runnable task = queue.poll(10, TimeUnit.SECONDS);
            if (task == null) continue;
            try {
                guarded(task, t -> t.run());
            } catch (Exception e) {
                log.error("Background disposer failed", e);
            }
        }
    }
}
