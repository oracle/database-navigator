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

package com.dbn.debugger.jdwp;

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Documents;
import com.dbn.connection.ConnectionHandler;
import com.dbn.debugger.DBDebugConsoleLogger;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dbn.debugger.common.breakpoint.DBBreakpointProperties;
import com.dbn.debugger.common.breakpoint.DBBreakpointType;
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.jdwp.process.DBJdwpDebugProcess;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.psql.PSQLFile;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getBreakpointManager;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.intellij.debugger.impl.PrioritizedTask.Priority.NORMAL;

public class DBJdwpBreakpointHandler extends DBBreakpointHandler<DBJdwpDebugProcess> {
    private static final ClassPrepareRequestor GENERIC_CLASS_PREPARE_REQUESTER = (p, r) -> {};
    private static final Key<LineBreakpoint> LINE_BREAKPOINT = Key.create("DBNavigator.LineBreakpoint");

    public DBJdwpBreakpointHandler(XDebugSession session, DBJdwpDebugProcess debugProcess) {
        super(session, debugProcess);
    }

    @Override
    public void registerDefaultBreakpoint(DBObjectRef<DBMethod> method) {
        DBEditableObjectVirtualFile mainDatabaseFile = DBDebugUtil.getMainDatabaseFile(method);
        if (mainDatabaseFile == null) return;

        DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) mainDatabaseFile.getMainContentFile();
        PSQLFile psqlFile = (PSQLFile) sourceCodeFile.getPsiFile();
        if (psqlFile == null) return;

        String methodName = method.getObjectName();
        DBObjectType methodType = method.getObjectType().getGenericType();
        BasePsiElement basePsiElement = psqlFile.lookupObjectDeclaration(methodType, methodName);
        if (basePsiElement == null) return;

        BasePsiElement subject = basePsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
        int offset = subject.getTextOffset();
        Document document = Documents.getDocument(psqlFile);
        if (document == null) return;

        int line = document.getLineNumber(offset);
        DBObjectRef<DBSchemaObject> schemaObject = DBDebugUtil.getMainDatabaseObject(method);
        if (schemaObject == null) return;

        registerBreakpoint(sourceCodeFile, line);
    }

    public void registerWrapperBreakpoint(DBObjectRef<DBMethod> wrapperMethod) {
        DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
        DBEditableObjectVirtualFile wrapperFile = databaseFileSystem.findOrCreateDatabaseFile(getProject(), wrapperMethod);
        if (wrapperFile == null) return;

        DBContentVirtualFile contentFile = wrapperFile.getContentFile(DBContentType.CODE);
        if (contentFile == null) return;

        UserDataKeys.WRAPPER_FILE.set(contentFile, true);
        registerBreakpoint(contentFile, 0);
    }

    private void registerBreakpoint(DBContentVirtualFile contentFile, int line) {
        Read.run(() -> {
            Project project = getProject();
            ConnectionHandler connection = contentFile.getConnection();

            DBJdwpBreakpointProperties breakpointProperties = new DBJdwpBreakpointProperties(connection);
            registerBreakpoint(contentFile, line, breakpointProperties);
        });
    }

    public static void registerBreakpoint(DBContentVirtualFile contentFile, int line, DBJdwpBreakpointProperties properties) {
        String fileUrl = contentFile.getUrl();
        Project project = contentFile.getProject();
        XBreakpointManager breakpointManager = getBreakpointManager(project);

        DBBreakpointType breakpointType = DBBreakpointType.get();
        breakpointManager.addLineBreakpoint(breakpointType, fileUrl, line, properties, true);
    }

    @Override
    public void unregisterDefaultBreakpoint() {

    }

    @Override
    protected void registerDatabaseBreakpoint(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint) {
        // not supported (see callback on class prepare)
    }

    private void createBreakpointRequest(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        DBDebugConsoleLogger console = getDebugProcess().getConsole();
        DBObjectRef databaseObject = DBBreakpointUtil.getDatabaseObject(breakpoint);
        String breakpointLocation = databaseObject == null ? "" : " on " + databaseObject.getQualifiedName() + " at line " + (breakpoint.getLine() + 1);
        try {
            VirtualMachineProxyImpl virtualMachineProxy = getVirtualMachineProxy();
            RequestManagerImpl requestsManager = getRequestsManager();

            String programIdentifier = DBBreakpointUtil.getProgramIdentifier(getConnection(), breakpoint);
            if (programIdentifier == null) return;

            LineBreakpoint lineBreakpoint = getLineBreakpoint(getSession().getProject(), breakpoint);
            if (lineBreakpoint == null || isBreakpointRequested(lineBreakpoint)) return;

            boolean registered = false;
            List<ReferenceType> referenceTypes = virtualMachineProxy.classesByName(programIdentifier);
            if (!referenceTypes.isEmpty()) {
                ReferenceType referenceType = referenceTypes.get(0);
                List<Location> locations = referenceType.locationsOfLine(breakpoint.getLine() + 1);
                if (!locations.isEmpty()) {
                    Location location = locations.get(0);
                    BreakpointRequest breakpointRequest = requestsManager.createBreakpointRequest(lineBreakpoint, location);
                    breakpointRequest.addThreadFilter(getMainThread());
                    requestsManager.enableRequest(breakpointRequest);
                    registered = true;
                }
            }

            if (!registered) {
                console.warning("Failed to register breakpoint" + breakpointLocation + ". Resource not found");
            }
        } catch (Exception e) {
            conditionallyLog(e);
            console.error("Failed to register breakpoint" + breakpointLocation + ". " + nvl(e.getMessage(), ""));
        }
    }

    private boolean isBreakpointRequested(LineBreakpoint lineBreakpoint) {
        RequestManagerImpl requestsManager = getRequestsManager();
        Set<EventRequest> requests = requestsManager.findRequests(lineBreakpoint);
        for (EventRequest request : requests) {
            if (request instanceof BreakpointRequest) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerBreakpoints(@NotNull List<XLineBreakpoint<XBreakpointProperties>> breakpoints, List<DBObjectRef<DBMethod>> methods) {
        registerMethodBreakpoints(methods);
        registerLineBreakpoints(breakpoints);
    }

    private void registerLineBreakpoints(@NotNull List<XLineBreakpoint<XBreakpointProperties>> breakpoints) {
        for (var breakpoint : breakpoints) {
            XBreakpointProperties properties = breakpoint.getProperties();
            if (properties instanceof DBBreakpointProperties breakpointProperties) {
                if (breakpointProperties.getConnection() == getConnection()) {
                    prepareObjectClasses(breakpoint);
                }
            } else if (properties instanceof JavaLineBreakpointProperties) {
                prepareObjectClasses(breakpoint);
            }
        }
    }

    private void registerMethodBreakpoints(List<DBObjectRef<DBMethod>> methods) {
        for (DBObjectRef<?> method : methods) {
            if (!method.isSchemaObject()) {
                method = method.getParentRef(o -> o.isSchemaObject());
            }

            if (method != null && method.isSchemaObject()) {
                DBContentType contentType = method.getObjectType().getContentType();
                if (contentType == DBContentType.CODE) {
                    prepareObjectClasses(method, DBContentType.CODE);
                } else if (contentType == DBContentType.CODE_SPEC_AND_BODY) {
                    prepareObjectClasses(method, DBContentType.CODE_SPEC);
                    prepareObjectClasses(method, DBContentType.CODE_BODY);
                }
            }
        }
    }

    private void prepareObjectClasses(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint) {

        String programIdentifier = DBBreakpointUtil.getProgramIdentifier(getConnection(), breakpoint);
        if (programIdentifier == null) return;

        LineBreakpoint lineBreakpoint = getLineBreakpoint(getSession().getProject(), breakpoint);
        if (lineBreakpoint == null) return;

        RequestManagerImpl requestsManager = getRequestsManager();
        Set<EventRequest> requests = requestsManager.findRequests(lineBreakpoint);
        if (!requests.isEmpty()) return;

        ClassPrepareRequest request = requestsManager.createClassPrepareRequest((p, r) -> createBreakpointRequest(breakpoint), programIdentifier);
        if (request == null) return;

        requestsManager.enableRequest(request);
    }

    private void prepareObjectClasses(DBObjectRef object, final DBContentType contentType) {
        RequestManagerImpl requestsManager = getRequestsManager();
        String programIdentifier = DBBreakpointUtil.getProgramIdentifier(getConnection(), object, contentType);

        ClassPrepareRequest request = requestsManager.createClassPrepareRequest(GENERIC_CLASS_PREPARE_REQUESTER, programIdentifier);
        if (request == null) return;

        requestsManager.enableRequest(request);
    }

    @Override
    protected void unregisterDatabaseBreakpoint(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint, final boolean temporary) {
        DBJdwpDebugProcess debugProcess = getDebugProcess();
        debugProcess.queueCommand(NORMAL, () -> {
            RequestManagerImpl requestsManager = getRequestsManager();
            LineBreakpoint lineBreakpoint = getLineBreakpoint(getSession().getProject(), breakpoint);
            if (temporary) {
                final Set<EventRequest> requests = requestsManager.findRequests(lineBreakpoint);
                for (EventRequest request : requests) {
                    request.disable();
                }

            } else {
                requestsManager.deleteRequest(lineBreakpoint);
            }
        });
    }

    @Nullable
    private static LineBreakpoint getLineBreakpoint(Project project, @NotNull XLineBreakpoint breakpoint) {
        LineBreakpoint lineBreakpoint = breakpoint.getUserData(LINE_BREAKPOINT);
        if (lineBreakpoint == null) {
            lineBreakpoint = createLineBreakpoint(project, breakpoint);
            breakpoint.putUserData(LINE_BREAKPOINT, lineBreakpoint);
        }
        return lineBreakpoint;
    }

    private static LineBreakpoint createLineBreakpoint(Project project, @NotNull XLineBreakpoint breakpoint) {
        return Read.call(() -> LineBreakpoint.create(project, breakpoint));
    }

    private ThreadReference getMainThread() {
        VirtualMachineProxyImpl virtualMachineProxy = getVirtualMachineProxy();
        ThreadReferenceProxyImpl threadReferenceProxy = virtualMachineProxy.allThreads().iterator().next();
        return threadReferenceProxy.getThreadReference();
    }

    private DebugProcessImpl getJdiDebugProcess() {
        return getDebugProcess().getDebuggerSession().getProcess();
    }

    private RequestManagerImpl getRequestsManager() {
        return getJdiDebugProcess().getRequestsManager();
    }

    private VirtualMachineProxyImpl getVirtualMachineProxy() {
        return getJdiDebugProcess().getVirtualMachineProxy();
    }
}
