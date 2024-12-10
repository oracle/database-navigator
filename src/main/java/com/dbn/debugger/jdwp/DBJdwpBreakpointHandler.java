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

import com.dbn.common.util.Documents;
import com.dbn.debugger.DBDebugConsoleLogger;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dbn.debugger.common.breakpoint.DBBreakpointProperties;
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.jdwp.process.DBJdwpDebugProcess;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.psql.PSQLFile;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.PrioritizedTask;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerUtil;
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

import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DBJdwpBreakpointHandler extends DBBreakpointHandler<DBJdwpDebugProcess> {
    private static final ClassPrepareRequestor GENERIC_CLASS_PREPARE_REQUESTER = (p, r) -> {};
    private static final Key<LineBreakpoint> LINE_BREAKPOINT = Key.create("DBNavigator.LineBreakpoint");

    public DBJdwpBreakpointHandler(XDebugSession session, DBJdwpDebugProcess debugProcess) {
        super(session, debugProcess);
    }

    @Override
    public void registerDefaultBreakpoint(DBMethod method) {
        DBEditableObjectVirtualFile mainDatabaseFile = DBDebugUtil.getMainDatabaseFile(method);
        if (mainDatabaseFile == null) return;

        DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) mainDatabaseFile.getMainContentFile();
        PSQLFile psqlFile = (PSQLFile) sourceCodeFile.getPsiFile();
        if (psqlFile == null) return;

        BasePsiElement basePsiElement = psqlFile.lookupObjectDeclaration(method.getObjectType().getGenericType(), method.getName());
        if (basePsiElement == null) return;

        BasePsiElement subject = basePsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
        int offset = subject.getTextOffset();
        Document document = Documents.getDocument(psqlFile);
        if (document == null) return;

        int line = document.getLineNumber(offset);
        DBSchemaObject schemaObject = DBDebugUtil.getMainDatabaseObject(method);
        if (schemaObject == null) return;

        XDebuggerUtil debuggerUtil = XDebuggerUtil.getInstance();
        debuggerUtil.toggleLineBreakpoint(getSession().getProject(), sourceCodeFile, line, true);
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
        DBSchemaObject databaseObject = DBBreakpointUtil.getDatabaseObject(breakpoint);
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
    public void registerBreakpoints(@NotNull List<XLineBreakpoint<XBreakpointProperties>> breakpoints, final List<? extends DBObject> objects) {
        ManagedThreadCommand.invoke(getJdiDebugProcess(), PrioritizedTask.Priority.LOW, () -> {
            for (DBObject object : objects) {
                if (object instanceof DBSchemaObject) {
                    DBSchemaObject schemaObject = (DBSchemaObject) object;
                    DBContentType contentType = schemaObject.getContentType();
                    if (contentType == DBContentType.CODE) {
                        prepareObjectClasses(schemaObject, DBContentType.CODE);
                    } else if (contentType == DBContentType.CODE_SPEC_AND_BODY) {
                        prepareObjectClasses(schemaObject, DBContentType.CODE_SPEC);
                        prepareObjectClasses(schemaObject, DBContentType.CODE_BODY);
                    }
                }
            }

            for (XLineBreakpoint<XBreakpointProperties> breakpoint : breakpoints) {
                XBreakpointProperties properties = breakpoint.getProperties();
                if (properties instanceof DBBreakpointProperties) {
                    DBBreakpointProperties breakpointProperties = (DBBreakpointProperties) properties;
                    if (breakpointProperties.getConnection() == getConnection()) {
                        prepareObjectClasses(breakpoint);
                    }
                }
            }
        });
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
        if (request != null) {
            requestsManager.enableRequest(request);
        }
    }

    private void prepareObjectClasses(final DBSchemaObject object, final DBContentType contentType) {
        RequestManagerImpl requestsManager = getRequestsManager();
        String programIdentifier = DBBreakpointUtil.getProgramIdentifier(getConnection(), object, contentType);

        ClassPrepareRequest request = requestsManager.createClassPrepareRequest(GENERIC_CLASS_PREPARE_REQUESTER, programIdentifier);
        if (request != null) {
            requestsManager.enableRequest(request);
        }
    }

    @Override
    protected void unregisterDatabaseBreakpoint(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint, final boolean temporary) {
        ManagedThreadCommand.invoke(getJdiDebugProcess(), PrioritizedTask.Priority.LOW, () -> {
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
            lineBreakpoint = LineBreakpoint.create(project, breakpoint);
            breakpoint.putUserData(LINE_BREAKPOINT, lineBreakpoint);
        }
        return lineBreakpoint;
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
