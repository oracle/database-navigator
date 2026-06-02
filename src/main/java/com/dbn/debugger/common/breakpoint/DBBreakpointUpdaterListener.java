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

package com.dbn.debugger.common.breakpoint;

import com.dbn.common.compatibility.Workaround;
import com.dbn.editor.code.SourceCodeManagerListener;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getAllLineBreakpoints;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getBreakpointFile;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.registerBreakpoint;

/**
 * WORKAROUND: Breakpoints do not seem to be registered properly in
 * the LineBreakpointManager given contents are loaded asynchronously.
 * This way the breakpoints get updated as soon as the file content is loaded.
 */
@Workaround
public class DBBreakpointUpdaterListener implements SourceCodeManagerListener {

    @Override
    public void sourceCodeLoaded(@NotNull DBSourceCodeVirtualFile file, boolean initialLoad) {
        if (!DEBUGGING.isSupported(file)) return;
        if (!initialLoad) return;

        guarded(() -> registerBreakpoints(file));
    }

    private static void registerBreakpoints(@NotNull DBSourceCodeVirtualFile file) {
        Project project = file.getProject();
        DBEditableObjectVirtualFile databaseFile = file.getMainDatabaseFile();
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);

        XBreakpointManager breakpointManager = debuggerManager.getBreakpointManager();
        for (XLineBreakpoint<?> breakpoint : getAllLineBreakpoints(project)) {
            VirtualFile virtualFile = getBreakpointFile(breakpoint);
            if (Objects.equals(virtualFile, databaseFile)) {
                breakpointManager.removeBreakpoint(breakpoint);
                registerBreakpoint(file,
                        breakpoint.getLine(),
                        breakpoint.isEnabled(),
                        breakpoint.isTemporary());
            }
        }
    }
}
