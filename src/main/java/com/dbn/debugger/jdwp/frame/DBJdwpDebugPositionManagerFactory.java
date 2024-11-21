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

package com.dbn.debugger.jdwp.frame;

import com.dbn.debugger.jdwp.process.DBJdwpDebugProcess;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.PositionManagerFactory;
import com.intellij.debugger.engine.DebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DBJdwpDebugPositionManagerFactory extends PositionManagerFactory {
    @Nullable
    @Override
    public PositionManager createPositionManager(@NotNull DebugProcess process) {
        DBJdwpDebugProcess jdwpDebugProcess = process.getUserData(DBJdwpDebugProcess.KEY);
        return jdwpDebugProcess == null ? null : new DBJdwpDebugPositionManager(process);
    }
}
