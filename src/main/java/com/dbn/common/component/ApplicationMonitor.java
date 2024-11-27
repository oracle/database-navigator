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

package com.dbn.common.component;

import com.dbn.common.event.ApplicationEvents;
import com.intellij.ide.AppLifecycleListener;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ApplicationMonitor {
    private static final ApplicationMonitor INSTANCE = new ApplicationMonitor();

    private final AtomicBoolean exitRequested = new AtomicBoolean(false);
    private final AtomicBoolean exiting = new AtomicBoolean(false);

    private ApplicationMonitor() {
        ApplicationEvents.subscribe(null, AppLifecycleListener.TOPIC, new AppLifecycleListener() {
            @Override
            public void appWillBeClosed(boolean isRestart) {
                exiting.set(true);
            }

            @Override
            public void appClosing() {
                exitRequested.set(true);
            }
        });
    }

    public static boolean isAppExiting() {
        return INSTANCE.exiting.get();
    }

    public static boolean checkAppExitRequested() {
        return INSTANCE.exitRequested.getAndSet(false);
    }
}
