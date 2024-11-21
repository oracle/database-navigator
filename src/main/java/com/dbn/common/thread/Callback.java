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

package com.dbn.common.thread;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.routine.Consumer;
import com.dbn.common.routine.ThrowableRunnable;
import com.intellij.openapi.project.Project;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class Callback{
    private Runnable before;
    private Runnable success;
    private Consumer<Exception> failure;
    private Runnable after;

    public static Callback create() {
        return new Callback();
    }

    public void before(Runnable before) {
        this.before = before;
    }

    public void onSuccess(Runnable success) {
        this.success = success;
    }

    public void onFailure(Consumer<Exception> failure) {
        this.failure = failure;
    }

    public void after(Runnable after) {
        this.after = after;
    }

    public void background(Project project, ThrowableRunnable<Exception> action) {
        Background.run(project, () -> surround(action));
    }

    public void surround(ThrowableRunnable<Exception> action) {
        try {
            Failsafe.guarded(before, b -> b.run());
            Failsafe.guarded(action);
            Failsafe.guarded(success, s -> s.run());
        } catch (Exception e) {
            conditionallyLog(e);
            if (failure != null) Failsafe.guarded(failure, f -> f.accept(e));
        } finally {
            Failsafe.guarded(after, a -> a.run());
        }
    }


}
