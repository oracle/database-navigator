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

package com.dbn.debugger;

import com.dbn.common.thread.Threads;
import com.dbn.database.interfaces.DatabaseInterface.Runnable;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static com.dbn.common.notification.NotificationGroup.DEBUGGER;
import static com.dbn.common.notification.NotificationSupport.sendErrorNotification;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class DBDebugOperation {

    public static void run(@NotNull Project project, String title, Runnable runnable) {
        ExecutorService executorService = Threads.debugExecutor();
        executorService.submit( () -> {
            Thread currentThread = Thread.currentThread();
            int initialPriority = currentThread.getPriority();
            currentThread.setPriority(Thread.MIN_PRIORITY);
            try {
                runnable.run();
            } catch (Exception e) {
                conditionallyLog(e);
                sendErrorNotification(project, DEBUGGER, txt("ntf.debugger.error.ErrorPerformingOperation", title, e));
            } finally {
                currentThread.setPriority(initialPriority);
            }
        });
    }


}
