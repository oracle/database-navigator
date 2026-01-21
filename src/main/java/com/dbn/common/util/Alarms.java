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

package com.dbn.common.util;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.thread.Dispatch;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.intellij.util.Alarm.ThreadToUse.SWING_THREAD;

@UtilityClass
public class Alarms {

    public static void executeLater(int delayMillis, @NotNull Runnable runnable) {
        Alarm alarm = new Alarm(SWING_THREAD);
        alarm.addRequest(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                conditionallyLog(e);
            } finally {
                Disposer.dispose(alarm);
            }
        }, delayMillis);
    }

    public static void executeLater(int delay, TimeUnit delayUnit, Runnable runnable) {
        executeLater((int) delayUnit.toMillis(delay), runnable);
    }

    public static Alarm createAlarm(Disposable parentDisposable) {
        Failsafe.nd(parentDisposable);
        return new Alarm(SWING_THREAD, parentDisposable);
    }

    public static void alarmRequest(@NotNull Alarm alarm, long delayMillis, boolean cancelRequests, @NotNull Runnable runnable) {
        Dispatch.run(true, () -> {
            if (alarm.isDisposed()) return;
            if (cancelRequests) alarm.cancelAllRequests();
            if (alarm.isDisposed()) return;

            alarm.addRequest(runnable, delayMillis);
        });
    }
}
