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

package com.dbn.debugger.jdwp;

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.thread.Read;
import com.dbn.common.thread.Write;
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
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBVirtualFileBase;
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
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getBreakpointLocation;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getBreakpointManager;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getDatabaseObject;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getProgramIdentifier;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.debugger.impl.PrioritizedTask.Priority.NORMAL;

@Slf4j
public class DBJdwpBreakpointHandler extends DBBreakpointHandler<DBJdwpDebugProcess> {
    private static final ClassPrepareRequestor GENERIC_CLASS_PREPARE_REQUESTER = (p, r) -> {};
    private static final Key<LineBreakpoint> LINE_BREAKPOINT = Key.create("DBNavigator.LineBreakpoint");
    private XLineBreakpoint<XBreakpointProperties> defaultBreakpoint;

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
        DBObjectRef<DBObject> object = DBDebugUtil.getMainDatabaseObject(method);
        if (object == null) return;

        registerDefaultBreakpoint(sourceCodeFile, line);
    }

    /**
     * Registers a temporary entry breakpoint in the current anonymous block.
     *
     * @param sourceFile source file containing the block
     * @param line zero-based line in the source file
     */
    public void registerDefaultSourceBreakpoint(DBVirtualFileBase sourceFile, int line) {
        registerDefaultBreakpoint(sourceFile, line);
    }

    private synchronized void registerDefaultBreakpoint(DBVirtualFileBase sourceFile, int line) {
        if (!canSetBreakpoints()) return;

        unregisterDefaultBreakpoint();
        defaultBreakpoint = DBBreakpointUtil.registerBreakpoint(sourceFile, line, true, true);
    }

    public void registerWrapperBreakpoint(DBObjectRef<DBMethod> wrapperMethod) {
        DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
        DBEditableObjectVirtualFile wrapperFile = databaseFileSystem.findOrCreateDatabaseFile(getProject(), wrapperMethod);
        if (wrapperFile == null) return;

        DBContentVirtualFile contentFile = wrapperFile.getContentFile(DBContentType.CODE);
        if (contentFile == null) return;

        UserDataKeys.WRAPPER_FILE.set(contentFile, true);
        DBBreakpointUtil.registerBreakpoint(contentFile, 0, true, true);
    }

    @Override
    public synchronized void unregisterDefaultBreakpoint() {
        XLineBreakpoint<XBreakpointProperties> breakpoint = defaultBreakpoint;
        defaultBreakpoint = null;
        if (breakpoint == null) return;

        Project project = getProject();
        XBreakpointManager breakpointManager = getBreakpointManager(project);
        Write.run(project, () -> {
            XBreakpoint<?>[] breakpoints = breakpointManager.getAllBreakpoints();
            boolean registered = Arrays.asList(breakpoints).contains(breakpoint);
            if (!registered) return;

            breakpointManager.removeBreakpoint(breakpoint);
        });
    }

    @Override
    protected void registerDatabaseBreakpoint(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint) {
        // not supported (see callback on class prepare)
    }

    private void createBreakpointRequest(
            @NotNull XLineBreakpoint<XBreakpointProperties> breakpoint,
            @NotNull ReferenceType referenceType,
            boolean reportFailure) {
        // Bind to the exact class received from the prepare event. Anonymous
        // block class names are generated and are not stable identifiers.
        DBDebugConsoleLogger console = getConsole();
        DBObjectRef databaseObject = getDatabaseObject(breakpoint);
        try {
            DBJdwpDebugProcess<?> debugProcess = getDebugProcess();
            Integer breakpointLine = debugProcess.resolveBreakpointLine(breakpoint);
            if (breakpointLine == null) return;

            RequestManagerImpl requestsManager = getRequestsManager();

            LineBreakpoint lineBreakpoint = getLineBreakpoint(getSession().getProject(), breakpoint);
            if (lineBreakpoint == null) return;

            List<Location> locations = referenceType.locationsOfLine(breakpointLine + 1);
            if (locations.isEmpty()) {
                if (reportFailure) {
                    console.warning(databaseObject == null ?
                            txt("log.debugger.warning.FailedRegisteringBreakpointResourceNotFound") :
                            txt("log.debugger.warning.FailedRegisteringBreakpointResourceNotFoundAtLocation", databaseObject.getQualifiedName(), breakpoint.getLine() + 1));
                }
                return;
            }

            Location location = locations.get(0);
            if (isBreakpointRequested(lineBreakpoint, location)) return;

            BreakpointRequest breakpointRequest = requestsManager.createBreakpointRequest(lineBreakpoint, location);
            breakpointRequest.addThreadFilter(getMainThread());
            requestsManager.enableRequest(breakpointRequest);
            String sourceLocation = getBreakpointLocation(breakpoint);
            String targetLocation = referenceType.name() + ":" + (breakpointLine + 1);
            console.system(txt("log.debugger.info.BreakpointAdded", sourceLocation + " -> " + targetLocation));
        } catch (Exception e) {
            conditionallyLog(e);
            if (reportFailure) {
                console.error(databaseObject == null ?
                        txt("log.debugger.error.FailedRegisteringBreakpoint", nvl(e.getMessage(), "")) :
                        txt("log.debugger.error.FailedRegisteringBreakpointAtLocation", databaseObject.getQualifiedName(), breakpoint.getLine() + 1, nvl(e.getMessage(), "")));
            }
        }
    }

    private boolean isBreakpointRequested(LineBreakpoint lineBreakpoint, Location location) {
        RequestManagerImpl requestsManager = getRequestsManager();
        Set<EventRequest> requests = requestsManager.findRequests(lineBreakpoint);
        for (EventRequest request : requests) {
            if (request instanceof BreakpointRequest breakpointRequest &&
                    breakpointRequest.location().equals(location)) {
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

        DBJdwpDebugProcess debugProcess = getDebugProcess();
        Integer breakpointLine = debugProcess.resolveBreakpointLine(breakpoint);
        if (breakpointLine == null) return;

        String programIdentifier = getProgramIdentifier(getConnection(), breakpoint);
        if (programIdentifier == null) return;

        boolean anonymousBlock = getDatabaseObject(breakpoint) == null;
        String classPattern = anonymousBlock ? getAnonymousClassPattern(programIdentifier) : programIdentifier;

        LineBreakpoint lineBreakpoint = getLineBreakpoint(getSession().getProject(), breakpoint);
        if (lineBreakpoint == null) return;

        RequestManagerImpl requestsManager = getRequestsManager();
        ClassPrepareRequest request = requestsManager.createClassPrepareRequest(
                (p, referenceType) -> {
                    if (!anonymousBlock || isAnonymousBlock(referenceType, programIdentifier)) {
                        createBreakpointRequest(breakpoint, referenceType, true);
                    }
                },
                classPattern);
        if (request == null) return;

        // Install the prepare request before inspecting loaded classes so a class
        // cannot become visible between the lookup and request registration.
        requestsManager.enableRequest(request);

        // The anonymous-block class cannot exist before the block is executed.
        // Scanning all loaded classes only probes unrelated Oracle built-ins,
        // many of which do not expose source information.
        List<ReferenceType> referenceTypes = anonymousBlock ?
                List.of() :
                getVirtualMachineProxy().classesByName(programIdentifier);
        for (ReferenceType referenceType : referenceTypes) {
            createBreakpointRequest(breakpoint, referenceType, false);
        }

        log.debug("Waiting for JDWP class {} to register breakpoint {} ({} matching classes already loaded)",
                classPattern,
                getBreakpointLocation(breakpoint),
                referenceTypes.size());
    }

    /**
     * Anonymous-block identity is exposed through the JDWP source path rather
     * than guaranteed as part of the generated class name. Listen for all
     * Oracle-generated classes and narrow them down in {@link #isAnonymousBlock}.
     */
    private static String getAnonymousClassPattern(String programIdentifier) {
        int separatorIndex = programIdentifier.indexOf('.');
        return separatorIndex == -1 ?
                programIdentifier + "*" :
                programIdentifier.substring(0, separatorIndex + 1) + "*";
    }

    private static boolean isAnonymousBlock(ReferenceType referenceType, String programIdentifier) {
        try {
            for (String sourcePath : referenceType.sourcePaths(null)) {
                try {
                    DBJdwpSourcePath path = DBJdwpSourcePath.from(sourcePath);
                    if (path.isAnonymousBlock()) return true;
                } catch (Exception e) {
                    conditionallyLog(e);
                }
            }
        } catch (AbsentInformationException e) {
            // Fall through to the legacy class-name check.
        }

        return referenceType.name().startsWith(programIdentifier);
    }

    private void prepareObjectClasses(DBObjectRef object, final DBContentType contentType) {
        RequestManagerImpl requestsManager = getRequestsManager();
        String programIdentifier = getProgramIdentifier(getConnection(), object, contentType);

        ClassPrepareRequest request = requestsManager.createClassPrepareRequest(GENERIC_CLASS_PREPARE_REQUESTER, programIdentifier);
        if (request == null) return;

        requestsManager.enableRequest(request);
    }

    @Override
    protected void unregisterDatabaseBreakpoint(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint, final boolean temporary) {
        DBJdwpDebugProcess debugProcess = getDebugProcess();
        String breakpointLocation = getBreakpointLocation(breakpoint);
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

            getConsole().system(txt("log.debugger.info.BreakpointRemoved", breakpointLocation));
        });
    }

    private DBDebugConsoleLogger getConsole() {
        return getDebugProcess().getConsole();
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
