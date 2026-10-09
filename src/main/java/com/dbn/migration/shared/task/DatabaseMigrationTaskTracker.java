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

package com.dbn.migration.shared.task;

import com.dbn.common.component.ProjectComponent;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.component.ProjectManagerListener;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.common.component.ApplicationMonitor.checkAppExitRequested;
import static com.dbn.nls.NlsResources.txt;

/**
 * Runs migration tasks in the background and tracks their lifecycle.
 *
 * <p>The tracker deliberately counts the submitted task for its complete background
 * execution, including tasks that have not started running yet. This prevents the
 * project from closing during the small scheduling window between task submission
 * and execution.</p>
 */
public final class DatabaseMigrationTaskTracker extends ProjectComponentBase implements ProjectManagerListener {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseMigrationTaskTracker";

    private final Object lock = new Object();
    private final Set<TrackedTask> activeTasks = new HashSet<>();
    private final List<Runnable> idleCallbacks = new ArrayList<>();

    public DatabaseMigrationTaskTracker(@NotNull ProjectComponent parent) {
        super(parent.getProject(), COMPONENT_NAME);
        Disposer.register(parent, this);
    }

    public void run(@NotNull Runnable task, @NotNull Runnable cancellation) {
        TrackedTask trackedTask = new TrackedTask(cancellation);
        synchronized (lock) {
            activeTasks.add(trackedTask);
        }

        try {
            Background.run(() -> {
                try {
                    task.run();
                } finally {
                    complete(trackedTask);
                }
            });
        } catch (RuntimeException | Error e) {
            complete(trackedTask);
            throw e;
        }
    }

    private boolean hasRunningTasks() {
        synchronized (lock) {
            return !activeTasks.isEmpty();
        }
    }

    @Override
    public boolean canCloseProject() {
        if (!hasRunningTasks()) return true;

        boolean exitApp = checkAppExitRequested();
        Messages.showQuestionDialog(
                getProject(),
                txt("msg.shared.title.Warning"),
                txt("msg.migration.question.CloseProjectWithRunningTasks"),
                Messages.OPTIONS_YES_NO,
                0,
                option -> {
                    if (option == 0) {
                        cancelRunningTasks(() -> closeProject(exitApp));
                    }
                });
        return false;
    }

    private void cancelRunningTasks(@NotNull Runnable onFinished) {
        List<TrackedTask> tasks;
        synchronized (lock) {
            if (activeTasks.isEmpty()) {
                Dispatch.run(onFinished);
                return;
            }

            idleCallbacks.add(onFinished);
            tasks = new ArrayList<>(activeTasks);
        }

        tasks.forEach(TrackedTask::cancel);
    }

    private void complete(@NotNull TrackedTask task) {
        List<Runnable> callbacks = null;
        synchronized (lock) {
            if (!activeTasks.remove(task) || !activeTasks.isEmpty()) return;

            callbacks = new ArrayList<>(idleCallbacks);
            idleCallbacks.clear();
        }

        callbacks.forEach(Dispatch::run);
    }

    private static final class TrackedTask {
        private final Runnable cancellation;
        private final AtomicBoolean cancellationRequested = new AtomicBoolean();

        private TrackedTask(@NotNull Runnable cancellation) {
            this.cancellation = cancellation;
        }

        private void cancel() {
            if (cancellationRequested.compareAndSet(false, true)) {
                cancellation.run();
            }
        }
    }
}
