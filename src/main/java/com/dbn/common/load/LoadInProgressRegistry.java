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

package com.dbn.common.load;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.util.CollectionUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProcessCanceledException;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class LoadInProgressRegistry<T extends StatefulDisposable> extends StatefulDisposableBase {
    private final List<T> nodes = CollectionUtil.createConcurrentList();

    private LoadInProgressRegistry(Disposable parentDisposable) {
        Disposer.register(parentDisposable, this);
    }

    public void register(T node) {
        boolean startTimer = nodes.size() == 0;
        nodes.add(node);
        if (startTimer) {
            Timer reloader = new Timer("DBN - Object Browser (load in progress reload timer)");
            reloader.schedule(new RefreshTask(), 0, LoadInProgressIcon.ROLL_INTERVAL);
        }
    }

    private class RefreshTask extends TimerTask {
        @Override
        public void run() {
            for (T node : nodes) {
                try {
                    if (node.isDisposed()) {
                        nodes.remove(node);
                    } else {
                        LoadInProgressRegistry.this.notify(node);
                    }
                } catch (ProcessCanceledException e) {
                    conditionallyLog(e);
                    nodes.remove(node);
                }
            }

            if (nodes.isEmpty()) {
                cancel();
            }
        }
    }

    protected abstract void notify(T node);

    public static <T extends StatefulDisposable> LoadInProgressRegistry<T> create(StatefulDisposable parentDisposable, Notifier<T> notifier) {
        return new LoadInProgressRegistry<>(parentDisposable) {
            @Override
            protected void notify(T node) {
                notifier.notify(node);
            }
        };
    }

    @FunctionalInterface
    public interface Notifier<T extends StatefulDisposable> {
        void notify(T node);
    }

    @Override
    public void disposeInner() {
        nodes.clear();
    }

}
