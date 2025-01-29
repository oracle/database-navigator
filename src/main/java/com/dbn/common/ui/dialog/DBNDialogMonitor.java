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

package com.dbn.common.ui.dialog;

import com.intellij.util.containers.ContainerUtil;

import java.util.Set;

/**
 * A utility class that monitors the lifecycle of DBNDialog instances.
 * It keeps track of open dialogs and provides mechanisms to register and release them,
 * allowing the application to determine whether any DBNDialog instances are still active.
 *
 * @author Dan Cioca (Oracle)
 */
public class DBNDialogMonitor {
    private static final Set<DBNDialog> openDialogs = ContainerUtil.createWeakSet();

    public static void registerDialog(DBNDialog dialog) {
        openDialogs.add(dialog);
    }

    public static void releaseDialog(DBNDialog dialog) {
        openDialogs.remove(dialog);
    }

    public static boolean hasOpenDialogs() {
        return !openDialogs.isEmpty();
    }
}
