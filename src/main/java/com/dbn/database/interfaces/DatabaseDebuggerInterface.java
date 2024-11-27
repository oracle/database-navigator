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

package com.dbn.database.interfaces;

import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.debug.BasicOperationInfo;
import com.dbn.database.common.debug.BreakpointInfo;
import com.dbn.database.common.debug.BreakpointOperationInfo;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.database.common.debug.DebuggerSessionInfo;
import com.dbn.database.common.debug.DebuggerVersionInfo;
import com.dbn.database.common.debug.ExecutionBacktraceInfo;
import com.dbn.database.common.debug.ExecutionStatusInfo;
import com.dbn.database.common.debug.VariableInfo;
import com.dbn.editor.DBContentType;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

public interface DatabaseDebuggerInterface extends DatabaseInterface{

    DebuggerSessionInfo initializeSession(DBNConnection connection) throws SQLException;

    void initializeJdwpSession(DBNConnection connection, String host, String port) throws SQLException;

    void disconnectJdwpSession(DBNConnection connection) throws SQLException;

    DebuggerVersionInfo getDebuggerVersion(DBNConnection connection) throws SQLException;

    void enableDebugging(DBNConnection connection) throws SQLException;

    void disableDebugging(DBNConnection connection) throws SQLException;

    void attachSession(DBNConnection connection, String sessionId) throws SQLException;

    void detachSession(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo synchronizeSession(DBNConnection connection) throws SQLException;

    BreakpointInfo addProgramBreakpoint(String programOwner, String programName, String programType, int line, DBNConnection connection) throws SQLException;

    BreakpointInfo addSourceBreakpoint(int line, DBNConnection connection) throws SQLException;

    BreakpointOperationInfo removeBreakpoint(int breakpointId, DBNConnection connection) throws SQLException;

    BreakpointOperationInfo enableBreakpoint(int breakpointId, DBNConnection connection) throws SQLException;

    BreakpointOperationInfo disableBreakpoint(int breakpointId, DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stepOver(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stepInto(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stepOut(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo runToPosition(String programOwner, String programName, String programType, int line, DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stopExecution(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo resumeExecution(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo getRuntimeInfo(DBNConnection connection) throws SQLException;

    ExecutionStatusInfo getExecutionStatusInfo(DBNConnection connection) throws SQLException;

    VariableInfo getVariableInfo(String variableName, Integer frameNumber, DBNConnection connection) throws SQLException;

    BasicOperationInfo setVariableValue(String variableName, Integer frameNumber, String value, DBNConnection connection) throws SQLException;

    ExecutionBacktraceInfo getExecutionBacktraceInfo(DBNConnection connection) throws SQLException;

    String[] getRequiredPrivilegeNames();

    String getDebugConsoleTemplate(CodeStyleCaseSettings settings);

    String getRuntimeEventReason(int code);

    String getJdwpBlockIdentifier();
    String getJdwpProgramIdentifier(DBObjectType objectType, DBContentType contentType, String qualifiedObjectName);

    String getJdwpTypeName(String typeIdentifier);
}
