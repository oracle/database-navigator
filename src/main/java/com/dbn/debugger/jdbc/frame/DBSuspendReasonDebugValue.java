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

package com.dbn.debugger.jdbc.frame;

import com.dbn.common.icon.Icons;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;

public class DBSuspendReasonDebugValue extends DBJdbcDebugValue {
    DBSuspendReasonDebugValue(DBJdbcDebugStackFrame stackFrame) {
        super(stackFrame, null, "DEBUG_RUNTIME_EVENT", null, Icons.EXEC_MESSAGES_INFO);
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        DebuggerRuntimeInfo runtimeInfo = getDebugProcess().getRuntimeInfo();
        String reason = "Unknown";
        if (runtimeInfo != null) {
            DatabaseDebuggerInterface debuggerInterface = getDebugProcess().getDebuggerInterface();
            reason = runtimeInfo.getReason() +" (" + debuggerInterface.getRuntimeEventReason(runtimeInfo.getReason()) + ")";
        }
        node.setPresentation(Icons.EXEC_MESSAGES_INFO, null, reason, false);
    }
}
