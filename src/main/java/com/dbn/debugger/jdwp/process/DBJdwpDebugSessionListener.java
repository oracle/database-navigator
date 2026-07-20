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

package com.dbn.debugger.jdwp.process;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.ThreadContext;
import com.dbn.debugger.DBDebugUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.Location;

import static com.dbn.common.thread.ThreadProperty.DEBUGGER_NAVIGATION;
import static com.dbn.common.util.Modality.nonModal;

class DBJdwpDebugSessionListener implements XDebugSessionListener {
    private final DBJdwpDebugProcess process;
    private final XDebugSession session;

    public DBJdwpDebugSessionListener(DBJdwpDebugProcess process, XDebugSession session) {
        this.process = process;
        this.session = session;
    }

    @Override
    @ThreadContext(DEBUGGER_NAVIGATION)
    public void sessionPaused() {
        XSuspendContext suspendContext = session.getSuspendContext();
        if (suspendContext == null || !process.shouldSuspend(suspendContext)) {
            Dispatch.run(nonModal(), () -> session.stepInto());
            return;
        }

        XExecutionStack activeExecutionStack = suspendContext.getActiveExecutionStack();

        Location location = DBJdwpDebugProcess.getTopFrameLocation(activeExecutionStack);
        VirtualFile virtualFile = process.getVirtualFile(location);
        DBDebugUtil.openEditor(virtualFile);
    }

    @Override
    public void sessionResumed() {
    }

    @Override
    public void sessionStopped() {
    }

    @Override
    public void stackFrameChanged() {
    }

    @Override
    public void beforeSessionResume() {
    }

    @Override
    public void settingsChanged() {
    }

    @Override
    public void breakpointsMuted(boolean muted) {
    }
}
