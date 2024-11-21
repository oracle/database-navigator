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

package com.dbn.debugger.common.breakpoint;

import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointManager;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.guarded;

/**
 * WORKAROUND: Breakpoints do not seem to be registered properly in the XLineBreakpointManager.
 * This way the breakpoints get updated as soon as the file is opened.
 */
public class DBBreakpointUpdaterFileEditorListener extends DBNFileEditorManagerListener {
    @Override
    public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (file instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) file;
            guarded(() -> registerBreakpoints(source, databaseFile));
        }
    }

    private static void registerBreakpoints(@NotNull FileEditorManager source, DBEditableObjectVirtualFile databaseFile) {
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(source.getProject());
        XBreakpointManagerImpl breakpointManager = (XBreakpointManagerImpl) debuggerManager.getBreakpointManager();
        for (XBreakpoint breakpoint : breakpointManager.getAllBreakpoints()) {
            if (breakpoint instanceof XLineBreakpoint) {
                XLineBreakpoint lineBreakpoint = (XLineBreakpoint) breakpoint;
                DBBreakpointUtil.setBreakpointId(lineBreakpoint, null);
                VirtualFile virtualFile = DBBreakpointUtil.getVirtualFile(lineBreakpoint);
                if (databaseFile.equals(virtualFile)) {
                    XLineBreakpointManager lineBreakpointManager = breakpointManager.getLineBreakpointManager();
                    lineBreakpointManager.registerBreakpoint((XLineBreakpointImpl) lineBreakpoint, true);
                }
            }
        }
    }
}
