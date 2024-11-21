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

package com.dbn.database.oracle;

import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseDebuggerInterfaceImpl;
import com.dbn.database.common.debug.BasicOperationInfo;
import com.dbn.database.common.debug.BreakpointInfo;
import com.dbn.database.common.debug.BreakpointOperationInfo;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.database.common.debug.DebuggerSessionInfo;
import com.dbn.database.common.debug.DebuggerVersionInfo;
import com.dbn.database.common.debug.ExecutionBacktraceInfo;
import com.dbn.database.common.debug.ExecutionStatusInfo;
import com.dbn.database.common.debug.VariableInfo;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.editor.DBContentType;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;
import java.util.StringTokenizer;

import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.common.util.Unsafe.warned;
import static com.dbn.editor.code.content.GuardedBlockMarker.END_OFFSET_IDENTIFIER;
import static com.dbn.editor.code.content.GuardedBlockMarker.START_OFFSET_IDENTIFIER;

public class OracleDebuggerInterface extends DatabaseDebuggerInterfaceImpl implements DatabaseDebuggerInterface {
    public OracleDebuggerInterface(DatabaseInterfaces provider) {
        super("oracle_debug_interface.xml", provider);
    }

    @Override
    public void initializeJdwpSession(DBNConnection connection, String host, String port) throws SQLException {
        executeCall(connection, null, "initialize-session-debugging");
        warned(() -> executeCall(connection, null, "initialize-session-compiler-flags"));
        executeCall(connection, null, "connect-jdwp-session", host, port);
    }

    @Override
    public void disconnectJdwpSession(DBNConnection connection) throws SQLException {
        executeCall(connection, null, "disconnect-jdwp-session");
    }

    @Override
    public DebuggerSessionInfo initializeSession(DBNConnection connection) throws SQLException {
        executeCall(connection, null, "initialize-session-debugging");
        warned(() -> executeCall(connection, null, "initialize-session-compiler-flags"));
        return executeCall(connection, new DebuggerSessionInfo(), "initialize-session");
    }

    @Override
    public DebuggerVersionInfo getDebuggerVersion(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerVersionInfo(), "get-debugger-version");
    }

    @Override
    public void enableDebugging(DBNConnection connection) throws SQLException {
        executeCall(connection, null, "enable-debugging");
    }

    @Override
    public void disableDebugging(DBNConnection connection) throws SQLException {
        executeCall(connection, null, "disable-debugging");
    }

    @Override
    public void attachSession(DBNConnection connection, String sessionId) throws SQLException {
        executeCall(connection, null, "attach-session", sessionId);
    }

    @Override
    public void detachSession(DBNConnection connection) throws SQLException {
        executeCall(connection, null, "detach-session");
    }

    @Override
    public DebuggerRuntimeInfo synchronizeSession(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerRuntimeInfo(), "synchronize-session");
    }

    @Override
    public BreakpointInfo addProgramBreakpoint(String programOwner, String programName, String programType, int line, DBNConnection connection) throws SQLException {
        return executeCall(connection, new BreakpointInfo(), "add-program-breakpoint", programOwner, programName, programType, line + 1);
    }

    @Override
    public BreakpointInfo addSourceBreakpoint(int line, DBNConnection connection) throws SQLException {
        return executeCall(connection, new BreakpointInfo(), "add-source-breakpoint", line + 1);
    }

    @Override
    public BreakpointOperationInfo removeBreakpoint(int breakpointId, DBNConnection connection) throws SQLException {
        return executeCall(connection, new BreakpointOperationInfo(), "remove-breakpoint", breakpointId);
    }

    @Override
    public BreakpointOperationInfo enableBreakpoint(int breakpointId, DBNConnection connection) throws SQLException {
        return executeCall(connection, new BreakpointOperationInfo(), "enable-breakpoint", breakpointId);
    }

    @Override
    public BreakpointOperationInfo disableBreakpoint(int breakpointId, DBNConnection connection) throws SQLException {
        return executeCall(connection, new BreakpointOperationInfo(), "disable-breakpoint", breakpointId);
    }

    @Override
    public DebuggerRuntimeInfo stepOver(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerRuntimeInfo(), "step-over");
    }

    @Override
    public DebuggerRuntimeInfo stepInto(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerRuntimeInfo(), "step-into");
    }

    @Override
    public DebuggerRuntimeInfo stepOut(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerRuntimeInfo(), "step-out");
    }

    @Override
    public DebuggerRuntimeInfo runToPosition(String programOwner, String programName, String programType, int line, DBNConnection connection) throws SQLException {
        BreakpointInfo breakpointInfo = addProgramBreakpoint(programOwner, programName, programType, line, connection);
        DebuggerRuntimeInfo runtimeInfo = stepOut(connection);
        Integer breakpointId = breakpointInfo.getBreakpointId();
        if (breakpointId != null) {
            removeBreakpoint(breakpointId, connection);
        }
        return runtimeInfo;
    }

    @Override
    public DebuggerRuntimeInfo resumeExecution(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerRuntimeInfo(), "resume-execution");
    }

    @Override
    public DebuggerRuntimeInfo stopExecution(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerRuntimeInfo(), "stop-execution");
    }

    @Override
    public DebuggerRuntimeInfo getRuntimeInfo(DBNConnection connection) throws SQLException {
        return executeCall(connection, new DebuggerRuntimeInfo(), "get-runtime-info");
    }

    @Override
    public ExecutionStatusInfo getExecutionStatusInfo(DBNConnection connection) throws SQLException {
        return executeCall(connection, new ExecutionStatusInfo(), "get-execution-status-info");
    }

    @Override
    public VariableInfo getVariableInfo(String variableName, Integer frameNumber, DBNConnection connection) throws SQLException {
        return executeCall(connection, new VariableInfo(), "get-variable", variableName, frameNumber);
    }

    @Override
    public BasicOperationInfo setVariableValue(String variableName, Integer frameNumber, String value, DBNConnection connection) throws SQLException {
        return executeCall(connection, new BasicOperationInfo(), "set-variable-value", frameNumber, variableName, value);
    }

    @Override
    public ExecutionBacktraceInfo getExecutionBacktraceInfo(DBNConnection connection) throws SQLException {
        return executeCall(connection, new ExecutionBacktraceInfo(), "get-execution-backtrace-table");
    }

    @Override
    public String[] getRequiredPrivilegeNames() {
        return new String[]{"DEBUG CONNECT SESSION", "DEBUG ANY PROCEDURE"};
    }

    @Override
    public String getDebugConsoleTemplate(CodeStyleCaseSettings settings) {
        CodeStyleCaseOption kco = settings.getKeywordCaseOption();
        CodeStyleCaseOption oco = settings.getObjectCaseOption();
        return START_OFFSET_IDENTIFIER +
                kco.format("DECLARE\n") +
                END_OFFSET_IDENTIFIER +
                "    -- add your declarations here\n" +
                "\n" +
                "\n" +
                START_OFFSET_IDENTIFIER +
                kco.format("BEGIN\n") +
                END_OFFSET_IDENTIFIER +
                "    -- add your code here" +
                "\n" +
                "\n" +
                "\n    COMMIT;\n" +
                START_OFFSET_IDENTIFIER +
                kco.format("END;\n") +
                "/" +
                END_OFFSET_IDENTIFIER;
    }

    @Override
    public String getRuntimeEventReason(int code) {
        switch (code) {
            case 0: return "None";
            case 2: return "Interpreter starting";
            case 3: return  "Stopped at a breakpoint";
            case 6: return  "Stopped at procedure entry";
            case 7: return  "Procedure return";
            case 8: return  "Procedure is finished";
            case 9: return  "Reached a new line";
            case 10: return  "An interrupt occurred";
            case 11: return  "An exception was raised";
            case 15: return  "Interpreter is exiting";
            case 16: return  "Start exception-handler";
            case 17: return  "A timeout occurred";
            case 20: return  "Instantiation block";
            case 21: return  "Interpreter is aborting";
            case 25: return  "Interpreter is exiting";
            case 4: return   "Executing SQL";
            case 14: return  "Watched value changed";
            case 18: return  "An RPC started";
            case 19: return  "Unhandled exception";
        }

        return null;
    }

    @Override
    public String getJdwpBlockIdentifier() {
        return "$Oracle.Block";
    }

    @Override
    public String getJdwpProgramIdentifier(DBObjectType objectType, DBContentType contentType, String qualifiedObjectName) {
        String objectTypeName = "Unknown";
        switch (objectType) {
            case PACKAGE: objectTypeName = contentType == DBContentType.CODE_SPEC ? "PackageSpec" : "PackageBody"; break;
            case FUNCTION: objectTypeName = "Function"; break;
            case PROCEDURE: objectTypeName = "Procedure"; break;
            case DATABASE_TRIGGER: objectTypeName = "Trigger"; break;
            case DATASET_TRIGGER: objectTypeName = "Trigger"; break;
            case TYPE: objectTypeName = contentType == DBContentType.CODE_SPEC ? "TypeSpec" : "TypeBody"; break;
        }
        return "$Oracle." + objectTypeName + "." + qualifiedObjectName;
    }

    @Override
    public String getJdwpTypeName(String typeIdentifier) {
        StringTokenizer tokenizer = new StringTokenizer(typeIdentifier, "\\.");
        if (tokenizer.countTokens() > 2) {
            String signature = tokenizer.nextToken();
            String programType = tokenizer.nextToken();
            StringBuilder typeName = new StringBuilder();
            while (tokenizer.hasMoreTokens()) {
                if (typeName.length() > 0) typeName.append(".");
                typeName.append(tokenizer.nextToken());
            }
            return cachedLowerCase(typeName.toString());
        }

        return typeIdentifier;
    }
}
