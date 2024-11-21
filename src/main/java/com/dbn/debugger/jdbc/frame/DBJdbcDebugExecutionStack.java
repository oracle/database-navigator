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

import com.dbn.common.util.Strings;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.database.common.debug.ExecutionBacktraceInfo;
import com.dbn.debugger.jdbc.DBJdbcDebugProcess;
import com.dbn.execution.statement.StatementExecutionInput;
import com.intellij.xdebugger.frame.XExecutionStack;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DBJdbcDebugExecutionStack extends XExecutionStack {
    private final DBJdbcDebugStackFrame topFrame;
    private final DBJdbcDebugProcess debugProcess;

    DBJdbcDebugExecutionStack(DBJdbcDebugProcess debugProcess) {
        // WORKAROUND hide the single value "threads" dropdown
        // super(debugProcess.getName(), debugProcess.getIcon());
        super("", null);

        this.debugProcess = debugProcess;
        ExecutionBacktraceInfo backtraceInfo = debugProcess.getBacktraceInfo();
        int frameNumber = backtraceInfo == null ? 1 : backtraceInfo.getTopFrameIndex();
        topFrame = new DBJdbcDebugStackFrame(debugProcess, debugProcess.getRuntimeInfo(), frameNumber);

    }


    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
        List<DBJdbcDebugStackFrame> frames = new ArrayList<>();
        ExecutionBacktraceInfo backtraceInfo = debugProcess.getBacktraceInfo();
        if (backtraceInfo == null) return;

        for (DebuggerRuntimeInfo runtimeInfo : backtraceInfo.getFrames()) {
            if (Strings.isNotEmpty(runtimeInfo.getOwnerName()) || debugProcess.getExecutionInput() instanceof StatementExecutionInput) {
                DBJdbcDebugStackFrame frame = new DBJdbcDebugStackFrame(debugProcess, runtimeInfo, runtimeInfo.getFrameIndex());
                frames.add(frame);
            }
        }
        container.addStackFrames(frames, true) ;
    }
}
