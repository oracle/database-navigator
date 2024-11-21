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

import com.dbn.common.file.util.VirtualFiles;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dbn.common.action.UserDataKeys.BREAKPOINT_FILE;
import static com.dbn.common.action.UserDataKeys.BREAKPOINT_ID;

public class DBBreakpointUtil {

    public static Integer getBreakpointId(@NotNull XLineBreakpoint breakpoint) {
        return breakpoint.getUserData(BREAKPOINT_ID);
    }

    public static void setBreakpointId(@NotNull XLineBreakpoint breakpoint, Integer id) {
        breakpoint.putUserData(BREAKPOINT_ID, id);
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull XLineBreakpoint breakpoint) {
        VirtualFile breakpointFile = breakpoint.getUserData(BREAKPOINT_FILE);
        if (breakpointFile != null) return breakpointFile;

        DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
        String fileUrl = breakpoint.getFileUrl();
        if (databaseFileSystem.isDatabaseUrl(fileUrl)) {
            VirtualFile virtualFile = databaseFileSystem.findFileByPath(fileUrl);
            if (virtualFile instanceof DBContentVirtualFile) {
                DBContentVirtualFile contentVirtualFile = (DBContentVirtualFile) virtualFile;
                breakpointFile = contentVirtualFile.getMainDatabaseFile();
                breakpoint.putUserData(BREAKPOINT_FILE, breakpointFile);
            } else if (virtualFile instanceof DBConsoleVirtualFile) {
                breakpointFile = virtualFile;
                breakpoint.putUserData(BREAKPOINT_FILE, breakpointFile);
            }
        } else {
            return VirtualFiles.findFileByUrl(fileUrl);
        }
        return breakpointFile;
    }

    @Nullable
    public static DBSchemaObject getDatabaseObject(@NotNull XLineBreakpoint breakpoint) {
        VirtualFile virtualFile = getVirtualFile(breakpoint);
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile objectVirtualFile = (DBEditableObjectVirtualFile) virtualFile;
            return objectVirtualFile.getObject();
        }
        return null;
    }

    public static DBContentType getContentType(@NotNull XLineBreakpoint breakpoint) {
        DBContentType contentType = DBContentType.CODE;
        VirtualFile virtualFile = getVirtualFile(breakpoint);
        if (virtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) virtualFile;
            contentType = sourceCodeFile.getContentType();
        }
        return contentType;
    }

    @Nullable
    public static String getProgramIdentifier(@NotNull ConnectionHandler connection, @NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        DBSchemaObject object = getDatabaseObject(breakpoint);
        DBContentType contentType = getContentType(breakpoint);
        return getProgramIdentifier(connection, object, contentType);
    }

    @Nullable
    public static String getProgramIdentifier(@NotNull ConnectionHandler connection, DBSchemaObject object, DBContentType contentType) {
        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
        return object == null ?
                debuggerInterface.getJdwpBlockIdentifier() :
                debuggerInterface.getJdwpProgramIdentifier(object.getObjectType(), contentType, object.getQualifiedName());
    }

    @NotNull
    public static String getBreakpointDesc(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        DBSchemaObject object = getDatabaseObject(breakpoint);
        VirtualFile virtualFile = getVirtualFile(breakpoint);
        int line = breakpoint.getLine() + 1;
        Integer breakpointId = getBreakpointId(breakpoint);
        String base = object == null ?
                virtualFile == null ? "unknown" : virtualFile.getName():
                object.getQualifiedName();

        return base + ":" + line + " (id=" + breakpointId + ")";
    }

    public static List<XLineBreakpoint<XBreakpointProperties>> getDatabaseBreakpoints(ConnectionHandler connection) {
        Project project = connection.getProject();
        Collection<XLineBreakpoint<XBreakpointProperties>> allBreakpoints = getAllBreakpoints(project);

        List<XLineBreakpoint<XBreakpointProperties>> breakpoints = new ArrayList<>();
        for (val breakpoint : allBreakpoints) {
            XBreakpointProperties properties = breakpoint.getProperties();
            if (properties instanceof DBBreakpointProperties) {
                DBBreakpointProperties breakpointProperties = (DBBreakpointProperties) properties;
                if (connection == breakpointProperties.getConnection()) {
                    breakpoints.add(breakpoint);
                }
            }
        }
        return breakpoints;
    }

    @NotNull
    private static Collection<XLineBreakpoint<XBreakpointProperties>> getAllBreakpoints(Project project) {
        DBBreakpointType databaseBreakpointType = XDebuggerUtil.getInstance().findBreakpointType(DBBreakpointType.class);
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        return Read.call(() -> Unsafe.cast(breakpointManager.getBreakpoints(databaseBreakpointType)));
    }
}
