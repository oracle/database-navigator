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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.editor.DBContentType;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.action.UserDataKeys.BREAKPOINT_FILE;
import static com.dbn.common.action.UserDataKeys.BREAKPOINT_ID;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.debugger.common.breakpoint.DBBreakpointType.createBreakpointProperties;

public class DBBreakpointUtil {

    public static final String JAVA_LINE_BREAKPOINT_TYPE_ID = "java-line";

    public static Integer getBreakpointId(@NotNull XLineBreakpoint breakpoint) {
        return breakpoint.getUserData(BREAKPOINT_ID);
    }

    public static void setBreakpointId(@NotNull XLineBreakpoint breakpoint, Integer id) {
        breakpoint.putUserData(BREAKPOINT_ID, id);
    }

    @Nullable
    public static VirtualFile getBreakpointFile(@NotNull XLineBreakpoint breakpoint) {
        VirtualFile breakpointFile = breakpoint.getUserData(BREAKPOINT_FILE);
        if (breakpointFile != null) return breakpointFile;

        DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
        String fileUrl = breakpoint.getFileUrl();
        if (databaseFileSystem.isDatabaseUrl(fileUrl)) {
            VirtualFile virtualFile = databaseFileSystem.findFileByPath(fileUrl);
            if (virtualFile instanceof DBContentVirtualFile contentVirtualFile) {
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
    public static DBObjectRef getDatabaseObject(@NotNull XLineBreakpoint breakpoint) {
        VirtualFile file = getBreakpointFile(breakpoint);
        if (file instanceof DBEditableObjectVirtualFile objectFile) {
            return objectFile.getObjectRef();
        }
        return null;
    }

    public static DBContentType getContentType(@NotNull XLineBreakpoint breakpoint) {
        DBContentType contentType = DBContentType.CODE;
        VirtualFile virtualFile = getBreakpointFile(breakpoint);
        if (virtualFile instanceof DBSourceCodeVirtualFile sourceCodeFile) {
            contentType = sourceCodeFile.getContentType();
        }
        return contentType;
    }

    @Nullable
    public static String getProgramIdentifier(@NotNull ConnectionHandler connection, @NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        DBObjectRef object = getDatabaseObject(breakpoint);
        DBContentType contentType = getContentType(breakpoint);
        return getProgramIdentifier(connection, object, contentType);
    }

    @Nullable
    public static String getProgramIdentifier(@NotNull ConnectionHandler connection, DBObjectRef object, DBContentType contentType) {
        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
        return object == null ?
                debuggerInterface.getJdwpBlockIdentifier() :
                debuggerInterface.getJdwpProgramIdentifier(object.getObjectType(), contentType, object.getQualifiedName());
    }

    @NotNull
    public static String getBreakpointDesc(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        DBObjectRef object = getDatabaseObject(breakpoint);
        VirtualFile virtualFile = getBreakpointFile(breakpoint);
        int line = breakpoint.getLine() + 1;
        Integer breakpointId = getBreakpointId(breakpoint);
        String base = object == null ?
                virtualFile == null ? "unknown" : virtualFile.getName():
                object.getQualifiedName();

        return base + ":" + line + " (id=" + breakpointId + ")";
    }

    public static List<XLineBreakpoint<XBreakpointProperties>> getDatabaseBreakpoints(ConnectionHandler connection, DBDebuggerType debuggerType) {
        Project project = connection.getProject();
        List<XLineBreakpoint<XBreakpointProperties>> breakpoints = new ArrayList<>();

        // db program breakpoints
        Collection<XLineBreakpoint<XBreakpointProperties>> databaseBreakpoints = getDatabaseBreakpoints(project);
        for (var breakpoint : databaseBreakpoints) {
            XBreakpointProperties properties = breakpoint.getProperties();
            if (properties instanceof DBBreakpointProperties breakpointProperties) {
                if (connection == breakpointProperties.getConnection()) {
                    breakpoints.add(breakpoint);
                }
            }
        }

        // db java breakpoints
        if (debuggerType == DBDebuggerType.JDWP) {
            Collection<XLineBreakpoint<XBreakpointProperties>> javaBreakpoints = getJavaBreakpoints(project);
            for (var breakpoint : javaBreakpoints) {
                String fileUrl = breakpoint.getFileUrl();
                if (!DatabaseFileSystem.isDatabaseFile(fileUrl)) continue;

                ConnectionId connectionId = DatabaseFileSystem.getConnectionId(fileUrl);
                if (Objects.equals(connectionId, connection.getConnectionId())) {
                    breakpoints.add(breakpoint);
                }
            }
        }
        return breakpoints;
    }

    public static List<XLineBreakpoint> getAllLineBreakpoints(Project project) {
        XBreakpoint<?>[] breakpoints = getAllBreakpoints(project);

        return Arrays
                .stream(breakpoints)
                .filter(b -> b instanceof XLineBreakpoint<?>)
                .map(b -> (XLineBreakpoint) b)
                .toList();
    }

    public static XBreakpoint<?>[] getAllBreakpoints(Project project) {
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
        XBreakpointManager breakpointManager = debuggerManager.getBreakpointManager();
        return Read.call(() -> breakpointManager.getAllBreakpoints());
    }


    @NotNull
    private static Collection<XLineBreakpoint<XBreakpointProperties>> getDatabaseBreakpoints(Project project) {
        DBBreakpointType breakpointType = DBBreakpointType.get();
        XBreakpointManager breakpointManager = getBreakpointManager(project);
        return Read.call(() -> cast(breakpointManager.getBreakpoints(breakpointType)));
    }

    @NotNull
    private static Collection<XLineBreakpoint<XBreakpointProperties>> getJavaBreakpoints(Project project) {
        XBreakpoint<?>[] allBreakpoints = getAllBreakpoints(project);
        List<XLineBreakpoint<XBreakpointProperties>> javaBreakpoints = new ArrayList<>();

        for (XBreakpoint<?> breakpoint : allBreakpoints) {
            if (isJavaLineBreakpoint(breakpoint)) {
                javaBreakpoints.add(cast(breakpoint));
            }
        }

        return javaBreakpoints;
    }

    private static boolean isJavaLineBreakpoint(XBreakpoint<?> breakpoint) {
        return Objects.equals(breakpoint.getType().getId(), JAVA_LINE_BREAKPOINT_TYPE_ID);
    }

    public static @NotNull XBreakpointManager getBreakpointManager(Project project) {
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
        return debuggerManager.getBreakpointManager();
    }

    public static void registerBreakpoint(DBContentVirtualFile contentFile, int line, boolean enabled, boolean temporary) {
        Read.run(() -> {
            ConnectionHandler connection = contentFile.getConnection();

            String fileUrl = contentFile.getUrl();
            Project project = contentFile.getProject();

            XBreakpointProperties properties = createBreakpointProperties(connection);
            DBBreakpointType breakpointType = DBBreakpointType.get();

            XBreakpointManager breakpointManager = getBreakpointManager(project);
            XLineBreakpoint<XBreakpointProperties> breakpoint = breakpointManager.addLineBreakpoint(breakpointType, fileUrl, line, properties, temporary);
            breakpoint.setEnabled(enabled);
        });
    }
}
